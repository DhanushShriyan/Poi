package com.poi.feature.create

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.poi.core.data.EventRepository
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.model.EventCategory
import com.poi.core.model.EventVisibility
import com.poi.core.model.NewEvent
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private data class DateChoice(val label: String, val dayOffset: Int)

@Composable
fun CreateEventScreen(
    repository: EventRepository,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by remember { mutableStateOf("") }
    var summary by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EventCategory.FESTIVAL) }
    var visibility by remember { mutableStateOf(EventVisibility.PUBLIC) }
    val dates = remember { listOf(DateChoice("Tomorrow", 1), DateChoice("This weekend", 3), DateChoice("Next week", 7)) }
    var dateChoice by remember { mutableStateOf(dates.first()) }
    var accepted by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val valid = title.isNotBlank() && summary.isNotBlank() && venue.isNotBlank() && address.isNotBlank() && accepted

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text("Create an event", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Share accurate details. Community listings are reviewed after publishing.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(22.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(6.dp))
                    Column {
                        Text("Poster scanner ready for cloud setup", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "For this offline APK, enter event details below.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        item {
            PoiSectionHeader("Basic details")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Event name") },
                singleLine = true,
                isError = showErrors && title.isBlank(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = summary,
                onValueChange = { summary = it.take(300) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("What should people know?") },
                minLines = 3,
                isError = showErrors && summary.isBlank(),
                supportingText = { Text("${summary.length}/300") },
            )
        }

        item {
            PoiSectionHeader("Category")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(EventCategory.entries.filterNot { it == EventCategory.ALL }) { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text("${item.symbol}  ${item.label}") },
                    )
                }
            }
        }

        item {
            PoiSectionHeader("When")
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(dates) { item ->
                    FilterChip(
                        selected = dateChoice == item,
                        onClick = { dateChoice = item },
                        leadingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                        label = { Text(item.label) },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "The test event uses a four-hour duration. Exact date and time pickers will be enabled with the production calendar flow.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            PoiSectionHeader("Where")
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = venue,
                onValueChange = { venue = it.take(100) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Venue name") },
                singleLine = true,
                isError = showErrors && venue.isBlank(),
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it.take(160) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Address or locality") },
                singleLine = true,
                isError = showErrors && address.isBlank(),
            )
        }

        item {
            PoiSectionHeader("Who can see it?")
            Spacer(Modifier.height(8.dp))
            VisibilityChoice(
                title = "Public",
                supporting = "Discoverable by everyone nearby",
                icon = Icons.Default.Public,
                selected = visibility == EventVisibility.PUBLIC,
                onClick = { visibility = EventVisibility.PUBLIC },
            )
            VisibilityChoice(
                title = "Selected circle",
                supporting = "Visible only to friends you select later",
                icon = Icons.Default.Groups,
                selected = visibility == EventVisibility.CIRCLE,
                onClick = { visibility = EventVisibility.CIRCLE },
            )
            VisibilityChoice(
                title = "Invite only",
                supporting = "People need a private invitation",
                icon = Icons.Default.Lock,
                selected = visibility == EventVisibility.INVITE_ONLY,
                onClick = { visibility = EventVisibility.INVITE_ONLY },
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { accepted = !accepted },
                verticalAlignment = Alignment.Top,
            ) {
                Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                Text(
                    "I confirm that these details are accurate, I have permission to list this event, and the event follows the community guidelines.",
                    modifier = Modifier.padding(top = 10.dp),
                    color = if (showErrors && !accepted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        item {
            Button(
                onClick = {
                    if (!valid) {
                        showErrors = true
                        return@Button
                    }
                    saving = true
                    val starts = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(dateChoice.dayOffset.toLong())
                    scope.launch {
                        val event = repository.createEvent(
                            NewEvent(
                                title = title,
                                summary = summary,
                                category = category,
                                startsAtMillis = starts,
                                endsAtMillis = starts + TimeUnit.HOURS.toMillis(4),
                                venue = venue,
                                address = address,
                                visibility = visibility,
                            ),
                        )
                        saving = false
                        onCreated(event.id)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !saving,
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.padding(4.dp))
                Text(if (saving) "Publishing…" else "Publish for review")
            }
        }
    }
}

@Composable
private fun VisibilityChoice(
    title: String,
    supporting: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        ),
        border = CardDefaults.outlinedCardBorder().takeIf { !selected },
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(6.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) Icon(Icons.Default.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

