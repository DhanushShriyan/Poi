package com.poi.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long?,
) {
    val fraction: Float?
        get() = totalBytes
            ?.takeIf { it > 0L }
            ?.let { (bytesDownloaded.toFloat() / it.toFloat()).coerceIn(0f, 1f) }
}

class ApkUpdateInstaller(context: Context) {
    enum class InstallResult {
        INSTALLER_OPENED,
        PERMISSION_REQUIRED,
        FAILED,
    }

    private val appContext = context.applicationContext

    suspend fun download(
        update: AppUpdate,
        onProgress: (DownloadProgress) -> Unit = {},
    ): Result<Uri> = try {
        val uri = withTimeout(DOWNLOAD_TIMEOUT_MS) {
            downloadWithRetry(update, onProgress)
        }
        Result.success(uri)
    } catch (error: TimeoutCancellationException) {
        Result.failure(IOException("The download timed out. Check your connection and retry."))
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    fun openInstaller(apkUri: Uri): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !appContext.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${appContext.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return runCatching { appContext.startActivity(settingsIntent) }
                .fold(
                    onSuccess = { InstallResult.PERMISSION_REQUIRED },
                    onFailure = { InstallResult.FAILED },
                )
        }

        val installIntent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, APK_MIME_TYPE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        return runCatching { appContext.startActivity(installIntent) }
            .fold(
                onSuccess = { InstallResult.INSTALLER_OPENED },
                onFailure = { InstallResult.FAILED },
            )
    }

    private suspend fun downloadWithRetry(
        update: AppUpdate,
        onProgress: (DownloadProgress) -> Unit,
    ): Uri {
        var lastNetworkError: IOException? = null
        repeat(MAX_DOWNLOAD_ATTEMPTS) { attempt ->
            try {
                return downloadOnce(update, onProgress)
            } catch (error: IOException) {
                lastNetworkError = error
                if (attempt < MAX_DOWNLOAD_ATTEMPTS - 1) delay(RETRY_DELAY_MS)
            }
        }
        throw checkNotNull(lastNetworkError)
    }

    private suspend fun downloadOnce(
        update: AppUpdate,
        onProgress: (DownloadProgress) -> Unit,
    ): Uri = withContext(Dispatchers.IO) {
        val updateDirectory = File(appContext.filesDir, UPDATE_DIRECTORY).apply {
            check(exists() || mkdirs()) { "Poi could not prepare storage for the update." }
        }
        val finalFile = File(updateDirectory, "poi-${safeFilePart(update.versionName)}.apk")
        val partialFile = File(updateDirectory, "${finalFile.name}.part")
        partialFile.delete()

        var connection: HttpURLConnection? = null
        try {
            connection = openDownloadConnection(update.downloadUrl)
            val totalBytes = connection.contentLengthLong.takeIf { it >= 0L }
            check(totalBytes == null || totalBytes <= MAX_APK_BYTES) {
                "The update file is unexpectedly large."
            }
            reportProgress(onProgress, DownloadProgress(0L, totalBytes))

            var downloadedBytes = 0L
            var lastProgressAt = 0L
            connection.inputStream.buffered().use { input ->
                FileOutputStream(partialFile).buffered().use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        downloadedBytes += count
                        check(downloadedBytes <= MAX_APK_BYTES) {
                            "The update file is unexpectedly large."
                        }

                        val now = SystemClock.elapsedRealtime()
                        if (now - lastProgressAt >= PROGRESS_INTERVAL_MS) {
                            reportProgress(
                                onProgress,
                                DownloadProgress(downloadedBytes, totalBytes),
                            )
                            lastProgressAt = now
                        }
                    }
                }
            }

            check(downloadedBytes > 0L) { "GitHub returned an empty update file." }
            check(totalBytes == null || downloadedBytes == totalBytes) {
                "The update download was incomplete."
            }
            reportProgress(onProgress, DownloadProgress(downloadedBytes, totalBytes))

            verifyApk(partialFile)
            finalFile.delete()
            check(partialFile.renameTo(finalFile)) { "Poi could not save the downloaded update." }
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.poi-update-files",
                finalFile,
            )
        } finally {
            connection?.disconnect()
            partialFile.delete()
        }
    }

    private fun openDownloadConnection(downloadUrl: String): HttpURLConnection {
        var currentUrl = URL(downloadUrl)
        requireHttps(currentUrl)

        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            val connection = (currentUrl.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                setRequestProperty("Accept", APK_MIME_TYPE)
                setRequestProperty("User-Agent", "Poi-Android-Updater")
            }

            when (val responseCode = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> return connection
                HttpURLConnection.HTTP_MOVED_PERM,
                HttpURLConnection.HTTP_MOVED_TEMP,
                HttpURLConnection.HTTP_SEE_OTHER,
                HTTP_TEMPORARY_REDIRECT,
                HTTP_PERMANENT_REDIRECT,
                -> {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    check(redirectCount < MAX_REDIRECTS && !location.isNullOrBlank()) {
                        "The GitHub download redirected too many times."
                    }
                    currentUrl = URL(currentUrl, location)
                    requireHttps(currentUrl)
                }
                else -> {
                    connection.disconnect()
                    throw IOException("GitHub returned HTTP $responseCode for this update.")
                }
            }
        }
        error("The GitHub download redirected too many times.")
    }

    @Suppress("DEPRECATION")
    private fun verifyApk(apkFile: File) {
        val packageManager = appContext.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val archiveInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
            ?: error("The downloaded file is not a valid Android app.")
        check(archiveInfo.packageName == appContext.packageName) {
            "The downloaded update is not an official Poi package."
        }

        val installedInfo = packageManager.getPackageInfo(appContext.packageName, flags)
        check(
            PackageInfoCompat.getLongVersionCode(archiveInfo) >
                PackageInfoCompat.getLongVersionCode(installedInfo),
        ) {
            "This update is not newer than the installed version."
        }

        val installedSignatures = installedInfo.signaturesForVerification()
        val archiveSignatures = archiveInfo.signaturesForVerification()
        check(installedSignatures.isNotEmpty() && archiveSignatures.isNotEmpty()) {
            "Poi could not verify the update signature."
        }
        check(installedSignatures.any { installed ->
            archiveSignatures.any { archive ->
                installed.toByteArray().contentEquals(archive.toByteArray())
            }
        }) {
            "The update signature does not match the installed Poi app."
        }
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.signaturesForVerification(): List<Signature> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            signingInfo?.let { info ->
                if (info.hasMultipleSigners()) {
                    info.apkContentsSigners.toList()
                } else {
                    info.signingCertificateHistory.toList()
                }
            }.orEmpty()
        } else {
            signatures?.toList().orEmpty()
        }

    private suspend fun reportProgress(
        onProgress: (DownloadProgress) -> Unit,
        progress: DownloadProgress,
    ) = withContext(Dispatchers.Main.immediate) {
        onProgress(progress)
    }

    private fun requireHttps(url: URL) {
        check(url.protocol.equals("https", ignoreCase = true)) {
            "Poi only accepts secure HTTPS update links."
        }
    }

    private fun safeFilePart(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "-")

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
        const val UPDATE_DIRECTORY = "updates"
        const val BUFFER_SIZE = 32 * 1024
        const val MAX_APK_BYTES = 200L * 1024L * 1024L
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 20_000
        const val DOWNLOAD_TIMEOUT_MS = 180_000L
        const val PROGRESS_INTERVAL_MS = 250L
        const val RETRY_DELAY_MS = 750L
        const val MAX_DOWNLOAD_ATTEMPTS = 2
        const val MAX_REDIRECTS = 5
        const val HTTP_TEMPORARY_REDIRECT = 307
        const val HTTP_PERMANENT_REDIRECT = 308
    }
}
