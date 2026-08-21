package com.dhanushshriyan.poi

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.poi.core.update.ApkUpdateInstaller
import com.poi.core.update.AppUpdate
import com.poi.core.update.DownloadProgress
import com.poi.core.update.GitHubUpdateClient
import com.poi.core.update.VersionComparator
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppUpdateScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { GitHubUpdateClient() }
    val installer = remember { ApkUpdateInstaller(context.applicationContext) }
    var latest by remember { mutableStateOf<AppUpdate?>(null) }
    var checking by remember { mutableStateOf(true) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf<DownloadProgress?>(null) }
    var downloadedApk by remember { mutableStateOf<Uri?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var checkRequest by remember { mutableIntStateOf(0) }

    LaunchedEffect(checkRequest) {
        checking = true
        failed = false
        message = null
        client.latestRelease()
            .onSuccess { release -> latest = release }
            .onFailure { error ->
                failed = true
                message = error.message ?: "Poi could not check GitHub for updates."
            }
        checking = false
    }

    val updateAvailable = latest?.let {
        VersionComparator.isNewer(it.versionName, BuildConfig.VERSION_NAME)
    } == true

    fun openInstaller(uri: Uri) {
        message = when (installer.openInstaller(uri)) {
            ApkUpdateInstaller.InstallResult.INSTALLER_OPENED ->
                "Android is ready to install the update. Approve it to finish."
            ApkUpdateInstaller.InstallResult.PERMISSION_REQUIRED ->
                "Allow Poi to install updates, return here, then tap Install again."
            ApkUpdateInstaller.InstallResult.FAILED ->
                "Android could not open the installer. Use the browser download below."
        }
    }

    fun openBrowser() {
        val release = latest ?: return
        val opened = runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, release.downloadUrl.toUri()))
        }.isSuccess
        failed = !opened
        message = if (opened) {
            "The official APK is open in your browser. Download it and approve installation."
        } else {
            "No browser could open the official APK link."
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("App updates") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (updateAvailable) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (updateAvailable) Icons.Default.CloudDownload else Icons.Default.CheckCircle,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.padding(6.dp))
                            Column {
                                Text(
                                    if (updateAvailable) "A new Poi is ready" else "Poi is up to date",
                                    style = MaterialTheme.typography.titleLarge,
                                )
                                Text(
                                    "Updates are signed and delivered from the public Poi GitHub repository.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        VersionRow("Installed version", BuildConfig.VERSION_NAME)
                        VersionRow(
                            "Latest available",
                            when {
                                checking -> "Checking…"
                                latest != null -> latest?.versionName.orEmpty()
                                else -> "Not available"
                            },
                        )

                        if (checking) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        if (downloading) {
                            val fraction = progress?.fraction
                            if (fraction == null) {
                                LinearProgressIndicator(Modifier.fillMaxWidth())
                            } else {
                                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                            }
                            Text(progress.updateDownloadStatus())
                        }
                        message?.let {
                            Text(
                                it,
                                color = if (failed) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            )
                        }

                        if (updateAvailable) {
                            Button(
                                enabled = !checking && !downloading && !BuildConfig.DEBUG,
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                onClick = {
                                    val existing = downloadedApk
                                    if (existing != null) {
                                        openInstaller(existing)
                                    } else {
                                        val release = latest ?: return@Button
                                        downloading = true
                                        failed = false
                                        message = null
                                        scope.launch {
                                            try {
                                                installer.download(release) { progress = it }
                                                    .onSuccess { uri ->
                                                        downloadedApk = uri
                                                        openInstaller(uri)
                                                    }
                                                    .onFailure { error ->
                                                        failed = true
                                                        message = error.message
                                                            ?: "The download failed. Try the browser option."
                                                    }
                                            } finally {
                                                downloading = false
                                            }
                                        }
                                    }
                                },
                            ) {
                                if (downloading) {
                                    CircularProgressIndicator(Modifier.padding(3.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CloudDownload, null)
                                }
                                Text(
                                    when {
                                        downloadedApk != null -> "  Install update"
                                        BuildConfig.DEBUG -> "  Available in production app"
                                        else -> "  Download and install"
                                    },
                                )
                            }
                            OutlinedButton(onClick = ::openBrowser, modifier = Modifier.fillMaxWidth()) {
                                Text("Download in browser instead")
                            }
                        } else {
                            FilledTonalButton(
                                enabled = !checking,
                                onClick = { checkRequest += 1 },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Refresh, null)
                                Text("  Check again")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "When an approved change reaches the main Git branch, Poi builds the next signed APK. " +
                        "This page checks that release and never installs an older or differently signed app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VersionRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun DownloadProgress?.updateDownloadStatus(): String {
    if (this == null) return "Connecting securely to GitHub…"
    val total = totalBytes
    return if (total == null) {
        "Downloaded ${formatUpdateBytes(bytesDownloaded)}"
    } else {
        val percent = ((fraction ?: 0f) * 100).roundToInt()
        "${formatUpdateBytes(bytesDownloaded)} of ${formatUpdateBytes(total)} · $percent%"
    }
}

private fun formatUpdateBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / (1024f * 1024f))
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024f)
    else -> "$bytes B"
}
