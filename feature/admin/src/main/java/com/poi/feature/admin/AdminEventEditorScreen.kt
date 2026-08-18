package com.poi.feature.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.data.EventRepository
import com.poi.core.model.EventCategory
import com.poi.core.model.EventVisibility
import com.poi.core.model.Organizer
import com.poi.core.model.VerificationLevel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val adminDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AdminEventEditorScreen(
    eventId: String,
    repository: EventRepository,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val events by repository.allEvents.collectAsStateWithLifecycle()
    val event = events.firstOrNull { it.id == eventId }
    if (event == null) {
        Scaffold(
            topBar = { TopAppBar(title = { Text("Event not found") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }) },
        ) { padding -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("This event is no longer available.") } }
        return
    }

    var title by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.title) }
    var summary by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.summary) }
    var description by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.description) }
    var venue by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.venue) }
    var address by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.address) }
    var organizer by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.organizer.name) }
    var starts by remember(event.id, event.updatedAtMillis) { mutableStateOf(formatDate(event.startsAtMillis)) }
    var ends by remember(event.id, event.updatedAtMillis) { mutableStateOf(formatDate(event.endsAtMillis)) }
    var attendees by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.attendeeCount.toString()) }
    var category by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.category) }
    var visibility by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.visibility) }
    var verification by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.verification) }
    var organizerVerified by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.organizer.isVerified) }
    var featured by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.featured) }
    var cancelled by remember(event.id, event.updatedAtMillis) { mutableStateOf(event.isCancelled) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this event?") },
            text = { Text("It will immediately disappear from discovery and plans on this device.") },
            confirmButton = {
                TextButton(onClick = { scope.launch { repository.deleteEvent(event.id); onSaved() } }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Keep event") } },
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Edit event") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.DeleteOutline, "Delete event", tint = MaterialTheme.colorScheme.error) } },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp, 10.dp, 20.dp, 44.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Listing details", style = MaterialTheme.typography.titleLarge)
                Text("Every field below is visible in the public or invited event experience.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { AdminTextField(title, { title = it.take(100) }, "Event title") }
            item { AdminTextField(summary, { summary = it.take(180) }, "Short summary", minLines = 2) }
            item { AdminTextField(description, { description = it.take(1200) }, "Full description", minLines = 4) }
            item { AdminTextField(organizer, { organizer = it.take(100) }, "Organizer") }
            item { AdminTextField(venue, { venue = it.take(100) }, "Venue") }
            item { AdminTextField(address, { address = it.take(180) }, "Address", minLines = 2) }
            item {
                AdminTextField(starts, { starts = it.take(16) }, "Starts (YYYY-MM-DD HH:mm)")
                AdminTextField(ends, { ends = it.take(16) }, "Ends (YYYY-MM-DD HH:mm)", modifier = Modifier.padding(top = 10.dp))
            }
            item {
                OutlinedTextField(
                    value = attendees,
                    onValueChange = { attendees = it.filter(Char::isDigit).take(7) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Attendee count") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
            item {
                AdminEnumPicker("Category", category, EventCategory.entries.filterNot { it == EventCategory.ALL }, { it.label }) { category = it }
                AdminEnumPicker("Visibility", visibility, EventVisibility.entries.toList(), { it.label }) { visibility = it }
                AdminEnumPicker("Verification", verification, VerificationLevel.entries.toList(), { it.label }) { verification = it }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        AdminSwitchRow("Verified organizer", organizerVerified) { organizerVerified = it }
                        AdminSwitchRow("Feature in discovery", featured) { featured = it }
                        AdminSwitchRow("Mark as cancelled", cancelled) { cancelled = it }
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Button(
                    onClick = {
                        error = null
                        val startsAt = parseDate(starts)
                        val endsAt = parseDate(ends)
                        when {
                            title.isBlank() || summary.isBlank() || venue.isBlank() || organizer.isBlank() -> error = "Complete the title, summary, organizer, and venue."
                            startsAt == null || endsAt == null -> error = "Use the date format YYYY-MM-DD HH:mm."
                            endsAt <= startsAt -> error = "The event must end after it starts."
                            else -> scope.launch {
                                repository.updateEvent(
                                    event.copy(
                                        title = title.trim(),
                                        summary = summary.trim(),
                                        description = description.trim(),
                                        organizer = Organizer(organizer.trim(), organizerVerified),
                                        venue = venue.trim(),
                                        address = address.trim(),
                                        startsAtMillis = startsAt,
                                        endsAtMillis = endsAt,
                                        attendeeCount = attendees.toIntOrNull() ?: 0,
                                        category = category,
                                        themeKey = category.name.lowercase(),
                                        visibility = visibility,
                                        verification = verification,
                                        featured = featured,
                                        isCancelled = cancelled,
                                    ),
                                )
                                onSaved()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text("Save all changes") }
            }
            item {
                OutlinedButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Default.DeleteOutline, null)
                    Text(" Delete event")
                }
            }
        }
    }
}

@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        minLines = minLines,
        singleLine = minLines == 1,
    )
}

@Composable
private fun <T> AdminEnumPicker(
    label: String,
    value: T,
    options: List<T>,
    valueLabel: (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        OutlinedTextField(
            value = valueLabel(value),
            onValueChange = {},
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
            label = { Text(label) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null) },
            enabled = false,
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(valueLabel(option)) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun AdminSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().height(52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatDate(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis)
    .atZone(ZoneId.systemDefault()).toLocalDateTime().format(adminDateFormatter)

private fun parseDate(value: String): Long? = runCatching {
    LocalDateTime.parse(value.trim(), adminDateFormatter)
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}.getOrNull()
