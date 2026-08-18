package com.poi.feature.admin

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.data.EventRepository
import com.poi.core.designsystem.PoiHeroPanel
import com.poi.core.model.Event
import com.poi.core.model.EventReport
import com.poi.core.model.VerificationLevel
import java.text.DateFormat
import kotlinx.coroutines.launch

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AdminDashboardScreen(
    repository: EventRepository,
    onBack: () -> Unit,
    onCreateEvent: () -> Unit,
    onEditEvent: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val events by repository.allEvents.collectAsStateWithLifecycle()
    val reports by repository.reportedEvents.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    val normalized = query.trim().lowercase()
    val visibleEvents = remember(events, normalized) {
        if (normalized.isBlank()) events else events.filter { event ->
            listOf(event.title, event.venue, event.organizer.name, event.address)
                .any { it.lowercase().contains(normalized) }
        }
    }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Administration") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCreateEvent) { Icon(Icons.Default.Add, "Create event") }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp, 10.dp, 20.dp, 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                PoiHeroPanel {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdminPanelSettings, null, tint = androidx.compose.ui.graphics.Color.White)
                        Spacer(Modifier.size(10.dp))
                        Text(
                            "Poi control centre",
                            color = androidx.compose.ui.graphics.Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Review community activity and keep every public listing accurate.",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.86f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    AdminMetric(Icons.Default.People, events.size.toString(), "Events", Modifier.weight(1f))
                    AdminMetric(Icons.Default.Verified, events.count { it.verification == VerificationLevel.OFFICIAL }.toString(), "Official", Modifier.weight(1f))
                    AdminMetric(Icons.Default.Flag, reports.size.toString(), "Reports", Modifier.weight(1f))
                }
            }
            if (reports.isNotEmpty()) {
                item {
                    Text("Needs attention", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Reported listings are hidden from public discovery until restored.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(reports, key = { it.eventId }) { report ->
                    val event = events.firstOrNull { it.id == report.eventId }
                    if (event != null) {
                        ReportCard(
                            event = event,
                            report = report,
                            onReview = { onEditEvent(event.id) },
                            onRestore = { scope.launch { repository.restoreReportedEvent(event.id) } },
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("All events", style = MaterialTheme.typography.titleLarge)
                    Button(onClick = onCreateEvent) {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.size(6.dp))
                        Text("New")
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search title, venue, organizer") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                )
            }
            items(visibleEvents, key = { it.id }) { event ->
                AdminEventRow(event = event, onEdit = { onEditEvent(event.id) })
            }
        }
    }
}

@Composable
private fun AdminMetric(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(9.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReportCard(event: Event, report: EventReport, onReview: () -> Unit, onRestore: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(event.title, fontWeight = FontWeight.SemiBold)
            Text("Reason: ${report.reason}", style = MaterialTheme.typography.bodyMedium)
            Text(
                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(report.reportedAtMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.72f),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onRestore) { Text("Restore") }
                TextButton(onClick = onReview) { Text("Review") }
            }
        }
    }
}

@Composable
private fun AdminEventRow(event: Event, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(event.category.symbol, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(13.dp))
            Column(Modifier.weight(1f)) {
                Text(event.title, fontWeight = FontWeight.SemiBold)
                Text(
                    "${event.venue} · ${event.verification.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (event.isCancelled) Text("Cancelled", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit ${event.title}") }
        }
    }
}
