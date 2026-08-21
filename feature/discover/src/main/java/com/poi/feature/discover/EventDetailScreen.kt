package com.poi.feature.discover

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.data.EventRepository
import com.poi.core.data.MomentRepository
import com.poi.core.designsystem.PoiEventArtwork
import com.poi.core.designsystem.PoiInitialAvatar
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.designsystem.PoiStatusPill
import com.poi.core.designsystem.formatEventDate
import com.poi.core.designsystem.formatEventTime
import com.poi.core.model.AttendanceStatus
import com.poi.core.model.CheckInVisibility
import com.poi.core.model.Event
import com.poi.core.model.EventVisibility
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailScreen(
    eventId: String,
    repository: EventRepository,
    momentRepository: MomentRepository,
    isAuthenticated: Boolean,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val events by repository.events.collectAsStateWithLifecycle()
    val attendance by repository.attendance.collectAsStateWithLifecycle()
    val settings by repository.settings.collectAsStateWithLifecycle()
    val event = events.firstOrNull { it.id == eventId }
    val status = attendance[eventId] ?: AttendanceStatus.NONE
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showCheckIn by remember { mutableStateOf(false) }
    var showReport by remember { mutableStateOf(false) }

    if (event == null) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("This event is no longer available.")
                TextButton(onClick = onBack) { Text("Go back") }
            }
        }
        return
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Event details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "${event.title}\n${formatEventDate(event.startsAtMillis)}\n${event.venue}, ${event.address}",
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share event"))
                    }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                    IconButton(onClick = { showReport = true }) {
                        Icon(Icons.Default.Flag, "Report event")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp),
        ) {
            item {
                PoiEventArtwork(event, Modifier.fillMaxWidth().height(250.dp).padding(horizontal = 16.dp))
            }
            item {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PoiStatusPill(event.category.label)
                        Spacer(Modifier.width(8.dp))
                        if (event.visibility != EventVisibility.PUBLIC) {
                            PoiStatusPill(event.visibility.label)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(event.title, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        event.summary,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(20.dp))

                    InfoRow(Icons.Default.CalendarMonth, "Date", formatEventDate(event.startsAtMillis))
                    InfoRow(
                        Icons.Default.Schedule,
                        "Time",
                        "${formatEventTime(event.startsAtMillis)} – ${formatEventTime(event.endsAtMillis)}",
                    )
                    InfoRow(Icons.Default.LocationOn, event.venue, event.address)

                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse("geo:0,0?q=${Uri.encode(event.address)}")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Directions, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open directions")
                    }

                    Spacer(Modifier.height(24.dp))
                    PoiSectionHeader("Your plan")
                    Spacer(Modifier.height(12.dp))
                    if (!isAuthenticated) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            shape = RoundedCornerShape(20.dp),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Keep this event close", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Sign in to save plans and share attendance with the audience you choose.",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(Modifier.height(10.dp))
                                Button(onClick = onSignIn) { Text("Sign in to plan") }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PlanButton(
                            text = "Interested",
                            selected = status == AttendanceStatus.INTERESTED,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!isAuthenticated) onSignIn() else scope.launch {
                                    repository.setAttendance(
                                        event.id,
                                        if (status == AttendanceStatus.INTERESTED) AttendanceStatus.NONE
                                        else AttendanceStatus.INTERESTED,
                                    )
                                }
                            },
                        )
                        PlanButton(
                            text = "Going",
                            selected = status == AttendanceStatus.GOING,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (!isAuthenticated) onSignIn() else scope.launch {
                                    repository.setAttendance(
                                        event.id,
                                        if (status == AttendanceStatus.GOING) AttendanceStatus.NONE
                                        else AttendanceStatus.GOING,
                                    )
                                }
                            },
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { if (isAuthenticated) showCheckIn = true else onSignIn() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.CheckCircle, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (status == AttendanceStatus.HERE) "Checked in" else "I'm here")
                    }

                    Spacer(Modifier.height(28.dp))
                    PoiSectionHeader("About this event")
                    Spacer(Modifier.height(10.dp))
                    Text(event.description, style = MaterialTheme.typography.bodyLarge)

                    Spacer(Modifier.height(24.dp))
                    OrganizerCard(event)

                    Spacer(Modifier.height(24.dp))
                    PoiSectionHeader("People you know")
                    Spacer(Modifier.height(10.dp))
                    if (event.friendNames.isEmpty()) {
                        Text(
                            "No friends have chosen to share their plan yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            event.friendNames.take(4).forEach { friend ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    PoiInitialAvatar(friend, Modifier.size(46.dp))
                                    Text(friend, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${event.attendeeCount} people are going. Only people who choose to be visible are named.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(24.dp))
                    EventMomentsSection(
                        eventId = event.id,
                        eventTitle = event.title,
                        repository = momentRepository,
                        attendanceStatus = status,
                        isAuthenticated = isAuthenticated,
                        onSignIn = onSignIn,
                    )

                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Your default check-in visibility is ${settings.defaultCheckInVisibility.label.lowercase()}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showCheckIn) {
        CheckInDialog(
            initial = settings.defaultCheckInVisibility,
            onDismiss = { showCheckIn = false },
            onConfirm = { visibility ->
                scope.launch { repository.setAttendance(event.id, AttendanceStatus.HERE, visibility) }
                showCheckIn = false
            },
        )
    }

    if (showReport) {
        ReportDialog(
            onDismiss = { showReport = false },
            onReport = { reason ->
                scope.launch { repository.reportEvent(event.id, reason) }
                showReport = false
                onBack()
            },
        )
    }
}

@Composable
private fun PlanButton(
    text: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    if (selected) {
        FilledTonalButton(onClick = onClick, modifier = modifier) { Text("✓ $text") }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(text) }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).background(
                MaterialTheme.colorScheme.primaryContainer,
                RoundedCornerShape(14.dp),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun OrganizerCard(event: Event) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PoiInitialAvatar(event.organizer.name, Modifier.size(48.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(event.organizer.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    event.verification.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (event.organizer.isVerified) {
                Icon(Icons.Default.CheckCircle, "Verified", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CheckInDialog(
    initial: CheckInVisibility,
    onDismiss: () -> Unit,
    onConfirm: (CheckInVisibility) -> Unit,
) {
    var selected by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Who can see you're here?") },
        text = {
            Column {
                Text("Your check-in expires automatically after the event.")
                Spacer(Modifier.height(12.dp))
                CheckInVisibility.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = option }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == option, onClick = { selected = option })
                        Column {
                            Text(option.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                when (option) {
                                    CheckInVisibility.PRIVATE -> "No one else can see this check-in"
                                    CheckInVisibility.FRIENDS -> "Only accepted friends can see it"
                                    CheckInVisibility.ATTENDEES -> "Visible to people in this event"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selected) }) { Text("Check in") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun ReportDialog(onDismiss: () -> Unit, onReport: (String) -> Unit) {
    val reasons = listOf("Wrong information", "Event was cancelled", "Spam or scam", "Unsafe content")
    var selected by remember { mutableStateOf(reasons.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report this event") },
        text = {
            Column {
                Text("The event will be hidden on this device after reporting.")
                Spacer(Modifier.height(10.dp))
                reasons.forEach { reason ->
                    Row(
                        Modifier.fillMaxWidth().clickable { selected = reason }.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected == reason, onClick = { selected = reason })
                        Text(reason)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onReport(selected) }) { Text("Report") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
