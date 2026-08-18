package com.poi.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class ApkUpdateInstaller(context: Context) {
    enum class InstallResult {
        INSTALLER_OPENED,
        PERMISSION_REQUIRED,
        FAILED,
    }

    private val appContext = context.applicationContext
    private val downloadManager = appContext.getSystemService(DownloadManager::class.java)

    suspend fun download(update: AppUpdate): Result<Uri> = runCatching {
        withContext(Dispatchers.IO) {
            val request = DownloadManager.Request(Uri.parse(update.downloadUrl))
                .setTitle("Poi ${update.versionName}")
                .setDescription("Downloading the latest Poi update")
                .setMimeType(APK_MIME_TYPE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(
                    appContext,
                    Environment.DIRECTORY_DOWNLOADS,
                    "poi-${update.versionName}-${System.currentTimeMillis()}.apk",
                )

            val downloadId = downloadManager.enqueue(request)
            var downloadedUri: Uri? = null
            while (downloadedUri == null) {
                downloadManager.query(DownloadManager.Query().setFilterById(downloadId)).use { cursor ->
                    check(cursor.moveToFirst()) { "The update download disappeared" }
                    when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            downloadedUri = checkNotNull(downloadManager.getUriForDownloadedFile(downloadId))
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(
                                cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON),
                            )
                            error("Update download failed with reason $reason")
                        }
                    }
                }
                if (downloadedUri == null) delay(750)
            }
            checkNotNull(downloadedUri)
        }
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

    private companion object {
        const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
