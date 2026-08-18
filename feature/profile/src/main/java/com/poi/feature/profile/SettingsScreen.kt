package com.poi.feature.profile

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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.data.EventRepository
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.designsystem.PoiSettingRow
import com.poi.core.model.CheckInVisibility
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: EventRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val settings by repository.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

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
                PoiSectionHeader("Notifications")
                PoiSettingRow(
                    icon = Icons.Default.Notifications,
                    title = "Event reminders",
                    supporting = "Important start and change alerts",
                    trailing = {
                        Switch(
                            checked = settings.eventReminders,
                            onCheckedChange = { value ->
                                scope.launch { repository.updateSettings(settings.copy(eventReminders = value)) }
                            },
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
            }
        }
    }
}

