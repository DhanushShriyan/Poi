package com.dhanushshriyan.poi

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.poi.core.update.ApkUpdateInstaller
import com.poi.core.update.AppUpdate
import com.poi.core.update.DownloadProgress
import com.poi.core.update.GitHubUpdateClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class UpdateUiState {
    READY,
    DOWNLOADING,
}

@Composable
internal fun PoiUpdatePrompt() {
    if (BuildConfig.DEBUG) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val updateClient = remember { GitHubUpdateClient() }
    val installer = remember { ApkUpdateInstaller(context.applicationContext) }
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var downloadedApk by remember { mutableStateOf<Uri?>(null) }
    var downloadProgress by remember { mutableStateOf<DownloadProgress?>(null) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }
    var uiState by remember { mutableStateOf(UpdateUiState.READY) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var downloadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        updateClient.findUpdate(BuildConfig.VERSION_NAME)
            .onSuccess { availableUpdate = it }
    }

    val update = availableUpdate ?: return

    fun openInstaller(apkUri: Uri) {
        statusMessage = when (installer.openInstaller(apkUri)) {
            ApkUpdateInstaller.InstallResult.INSTALLER_OPENED ->
                "Android's installer is ready. Approve it to finish updating Poi."
            ApkUpdateInstaller.InstallResult.PERMISSION_REQUIRED ->
                "Allow Poi to install updates, return here, then tap Install."
            ApkUpdateInstaller.InstallResult.FAILED ->
                "The installer could not be opened. Please try again."
        }
    }

    fun cancelDownload(message: String?) {
        downloadJob?.cancel()
        downloadJob = null
        downloadProgress = null
        uiState = UpdateUiState.READY
        statusMessage = message
    }

    fun openBrowserDownload() {
        val opened = runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)))
        }.isSuccess
        statusMessage = if (opened) {
            "The update was opened in your browser. Download it, then approve installation."
        } else {
            "No browser could open the update link. Please retry inside Poi."
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (uiState == UpdateUiState.DOWNLOADING) cancelDownload(null)
            availableUpdate = null
        },
        title = { Text("Poi ${update.versionName} is ready") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Download this update inside Poi. Android will ask you to approve installation.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (update.notes.isNotBlank()) {
                    Text(
                        text = update.notes.take(500),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (uiState == UpdateUiState.DOWNLOADING) {
                    val fraction = downloadProgress?.fraction
                    if (fraction == null) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                    } else {
                        LinearProgressIndicator(
                            progress = { fraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                        )
                    }
                    Text(downloadProgress.downloadStatus())
                }
                statusMessage?.let {
                    Text(
                        text = it,
                        color = if (downloadFailed) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
                if (downloadFailed) {
                    TextButton(onClick = ::openBrowserDownload) {
                        Text("Open download in browser")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = uiState != UpdateUiState.DOWNLOADING,
                onClick = {
                    val existingDownload = downloadedApk
                    if (existingDownload != null) {
                        openInstaller(existingDownload)
                    } else {
                        uiState = UpdateUiState.DOWNLOADING
                        statusMessage = null
                        downloadFailed = false
                        downloadProgress = null
                        downloadJob = scope.launch {
                            try {
                                installer.download(update) { progress ->
                                    downloadProgress = progress
                                }
                                    .onSuccess { uri ->
                                        downloadedApk = uri
                                        openInstaller(uri)
                                    }
                                    .onFailure { error ->
                                        downloadFailed = true
                                        statusMessage = error.message
                                            ?: "Download failed. Check your connection and retry."
                                    }
                            } finally {
                                uiState = UpdateUiState.READY
                                downloadJob = null
                            }
                        }
                    }
                },
            ) {
                Text(
                    when {
                        uiState == UpdateUiState.DOWNLOADING -> "Downloading"
                        downloadedApk != null -> "Install"
                        downloadFailed -> "Retry download"
                        else -> "Download update"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (uiState == UpdateUiState.DOWNLOADING) {
                        cancelDownload("Download cancelled. You can retry when ready.")
                    } else {
                        availableUpdate = null
                    }
                },
            ) {
                Text(if (uiState == UpdateUiState.DOWNLOADING) "Cancel" else "Later")
            }
        },
    )
}

private fun DownloadProgress?.downloadStatus(): String {
    if (this == null) return "Connecting securely to GitHub…"
    val total = totalBytes
    val amount = formatBytes(bytesDownloaded)
    return if (total == null) {
        "Downloading securely from GitHub · $amount"
    } else {
        val percent = ((fraction ?: 0f) * 100f).roundToInt()
        "Downloading securely from GitHub · $amount of ${formatBytes(total)} · $percent%"
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024f)
    else -> "$bytes B"
}
