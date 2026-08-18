package com.dhanushshriyan.poi

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
import com.poi.core.update.GitHubUpdateClient
import kotlinx.coroutines.launch

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
    var uiState by remember { mutableStateOf(UpdateUiState.READY) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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

    AlertDialog(
        onDismissRequest = {
            if (uiState != UpdateUiState.DOWNLOADING) availableUpdate = null
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
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                    )
                    Text("Downloading securely from GitHub…")
                }
                statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
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
                        scope.launch {
                            installer.download(update)
                                .onSuccess { uri ->
                                    downloadedApk = uri
                                    openInstaller(uri)
                                }
                                .onFailure {
                                    statusMessage = "Download failed. Check your connection and try again."
                                }
                            uiState = UpdateUiState.READY
                        }
                    }
                },
            ) {
                Text(
                    when {
                        uiState == UpdateUiState.DOWNLOADING -> "Downloading"
                        downloadedApk != null -> "Install"
                        else -> "Download update"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(
                enabled = uiState != UpdateUiState.DOWNLOADING,
                onClick = { availableUpdate = null },
            ) {
                Text("Later")
            }
        },
    )
}
