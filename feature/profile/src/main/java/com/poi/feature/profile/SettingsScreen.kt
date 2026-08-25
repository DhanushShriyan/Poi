package com.poi.feature.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.data.EventRepository
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.designsystem.PoiSettingRow
import com.poi.core.location.LocationRepository
import com.poi.core.model.CheckInVisibility
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: EventRepository,
    locationRepository: LocationRepository,
    deleteAccount: suspend () -> Result<Unit>,
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by repository.settings.collectAsStateWithLifecycle()
    val profile by repository.profile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var displayName by remember(profile.id, profile.displayName) { mutableStateOf(profile.displayName) }
    var handle by remember(profile.id, profile.handle) { mutableStateOf(profile.handle) }
    var homeArea by remember(profile.id, profile.homeArea) { mutableStateOf(profile.homeArea) }
    var accountMessage by remember { mutableStateOf<String?>(null) }
    var savingProfile by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteConfirmation by remember { mutableStateOf("") }
    var deletingAccount by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        scope.launch {
            repository.updateSettings(settings.copy(eventReminders = granted))
        }
    }
    val setEventReminders: (Boolean) -> Unit = { enabled ->
        if (!enabled) {
            scope.launch { repository.updateSettings(settings.copy(eventReminders = false)) }
        } else if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            scope.launch { repository.updateSettings(settings.copy(eventReminders = true)) }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Privacy & notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp),
        ) {
            item {
                PoiSectionHeader("Profile")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Display name") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it.take(25) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Handle") },
                    supportingText = { Text("3–24 letters, numbers, or underscores") },
                    singleLine = true,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = homeArea,
                    onValueChange = { homeArea = it.take(100) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("City or home area") },
                    singleLine = true,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    enabled = !savingProfile,
                    onClick = {
                        savingProfile = true
                        accountMessage = null
                        scope.launch {
                            runCatching { repository.updateProfile(displayName, handle, homeArea) }
                                .onSuccess { accountMessage = "Profile updated." }
                                .onFailure { accountMessage = it.message ?: "Profile could not be updated." }
                            savingProfile = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (savingProfile) "Saving…" else "Save profile") }
                accountMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        message,
                        color = if (message == "Profile updated.") {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                Spacer(Modifier.height(24.dp))
            }
            item {
                PoiSectionHeader("Default check-in visibility")
                Spacer(Modifier.height(6.dp))
                Text(
                    "You can change this separately whenever you check in.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                CheckInVisibility.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            scope.launch { repository.updateSettings(settings.copy(defaultCheckInVisibility = option)) }
                        }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = settings.defaultCheckInVisibility == option,
                            onClick = {
                                scope.launch { repository.updateSettings(settings.copy(defaultCheckInVisibility = option)) }
                            },
                        )
                        Column {
                            Text(option.label, style = MaterialTheme.typography.titleMedium)
                            Text(
                                when (option) {
                                    CheckInVisibility.PRIVATE -> "No one sees your check-in"
                                    CheckInVisibility.FRIENDS -> "Only accepted friends"
                                    CheckInVisibility.ATTENDEES -> "Other opted-in attendees"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                PoiSectionHeader("Social privacy")
                PoiSettingRow(
                    icon = Icons.Default.Group,
                    title = "Share future plans",
                    supporting = "Friends can see events you mark as going",
                    trailing = {
                        Switch(
                            checked = settings.showPlansToFriends,
                            onCheckedChange = { value ->
                                scope.launch { repository.updateSettings(settings.copy(showPlansToFriends = value)) }
                            },
                        )
                    },
                )
                PoiSettingRow(
                    icon = Icons.Default.PrivacyTip,
                    title = "Friend activity",
                    supporting = "Show opted-in friends on event pages",
                    trailing = {
                        Switch(
                            checked = settings.friendActivity,
                            onCheckedChange = { value ->
                                scope.launch { repository.updateSettings(settings.copy(friendActivity = value)) }
                            },
                        )
                    },
                )
                Spacer(Modifier.height(18.dp))
                PoiSectionHeader("Location privacy")
                PoiSettingRow(
                    icon = Icons.Default.LocationOn,
                    title = "Use current location for discovery",
                    supporting = "Calculate distance on your device while Poi is open",
                    trailing = {
                        Switch(
                            checked = settings.locationDiscoveryEnabled,
                            onCheckedChange = { value ->
                                if (!value) locationRepository.clear()
                                scope.launch {
                                    repository.updateSettings(settings.copy(locationDiscoveryEnabled = value))
                                }
                            },
                        )
                    },
                )
                PoiSettingRow(
                    icon = Icons.Default.MyLocation,
                    title = "Proximity-assisted check-in",
                    supporting = "Confirm that you are near the venue without retaining raw location",
                    trailing = {
                        Switch(
                            checked = settings.proximityCheckInEnabled,
                            onCheckedChange = { value ->
                                scope.launch {
                                    repository.updateSettings(settings.copy(proximityCheckInEnabled = value))
                                }
                            },
                        )
                    },
                )
                Text(
                    "Poi requests foreground location only. It does not track you in the background.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(18.dp))
                PoiSectionHeader("Notifications")
                PoiSettingRow(
                    icon = Icons.Default.Notifications,
                    title = "Event reminders",
                    supporting = "One private device alert about an hour before saved events",
                    trailing = {
                        Switch(
                            checked = settings.eventReminders,
                            onCheckedChange = setEventReminders,
                        )
                    },
                )
                PoiSettingRow(
                    icon = Icons.Default.Today,
                    title = "Weekly local digest",
                    supporting = "A short list of relevant weekend events",
                    trailing = {
                        Switch(
                            checked = settings.weeklyDigest,
                            onCheckedChange = { value ->
                                scope.launch { repository.updateSettings(settings.copy(weeklyDigest = value)) }
                            },
                        )
                    },
                )
                Spacer(Modifier.height(26.dp))
                PoiSectionHeader("Account control")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Deleting your account permanently removes your profile, attendance, comments, moments and private events. Public events may remain without an owner for community continuity.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Delete my account", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { if (!deletingAccount) showDeleteDialog = false },
            title = { Text("Permanently delete account?") },
            text = {
                Column {
                    Text("This cannot be undone. Type DELETE to confirm.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = deleteConfirmation,
                        onValueChange = { deleteConfirmation = it.take(6) },
                        label = { Text("Confirmation") },
                        singleLine = true,
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = deleteConfirmation == "DELETE" && !deletingAccount,
                    onClick = {
                        deletingAccount = true
                        accountMessage = null
                        scope.launch {
                            deleteAccount().fold(
                                onSuccess = {
                                    showDeleteDialog = false
                                    onAccountDeleted()
                                },
                                onFailure = {
                                    accountMessage = it.message ?: "Account could not be deleted."
                                    showDeleteDialog = false
                                },
                            )
                            deletingAccount = false
                        }
                    },
                ) { Text(if (deletingAccount) "Deleting…" else "Delete permanently") }
            },
            dismissButton = {
                OutlinedButton(
                    enabled = !deletingAccount,
                    onClick = { showDeleteDialog = false },
                ) { Text("Cancel") }
            },
        )
    }
}
