package com.poi.feature.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.data.EventRepository
import com.poi.core.data.searchAndFilter
import com.poi.core.designsystem.PoiEventCard
import com.poi.core.designsystem.PoiInitialAvatar
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.designsystem.PoiWordmark
import com.poi.core.model.AttendanceStatus
import com.poi.core.model.EventCategory
import com.poi.core.model.isLive
import kotlinx.coroutines.launch

@Composable
fun DiscoverScreen(
    repository: EventRepository,
    isGuest: Boolean,
    displayName: String?,
    onSignIn: () -> Unit,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val events by repository.events.collectAsStateWithLifecycle()
    val attendance by repository.attendance.collectAsStateWithLifecycle()
    val profile by repository.profile.collectAsStateWithLifecycle()
    val settings by repository.settings.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(EventCategory.ALL) }
    var showRadiusPicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val now = System.currentTimeMillis()
    val nearbyEvents = remember(events, settings.discoveryRadiusKm) {
        val radius = settings.discoveryRadiusKm
        if (radius == null) events else events.filter { it.distanceKm <= radius }
    }
    val filtered = remember(nearbyEvents, query, category) {
        nearbyEvents.searchAndFilter(query, category)
    }
    val browsingAll = query.isBlank() && category == EventCategory.ALL

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        PoiWordmark()
                        Spacer(Modifier.height(14.dp))
                        Text(
                            text = if (isGuest) "Good to see you" else "Hello, ${displayName ?: profile.displayName}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text("Find something worth showing up for", style = MaterialTheme.typography.headlineLarge)
                    }
                    if (isGuest) {
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(48.dp),
                            contentAlignment = androidx.compose.ui.Alignment.Center,
                        ) { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        PoiInitialAvatar(displayName ?: profile.displayName, Modifier.size(48.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                AssistChip(
                    onClick = { showRadiusPicker = true },
                    label = {
                        Text(
                            "${profile.homeArea}  ·  " +
                                (settings.discoveryRadiusKm?.let { "Within $it km" } ?: "Any distance"),
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(18.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.primary,
                    ),
                    border = null,
                )
            }
        }

        if (isGuest) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = MaterialTheme.shapes.large,
                ) {
                    androidx.compose.foundation.layout.Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Browse without an account", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Sign in only when you want to save a plan or join friends.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                        Button(onClick = onSignIn) { Text("Sign in") }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                placeholder = { Text("Search events, places, organizers") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
                ),
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 12.dp),
            ) {
                items(EventCategory.entries) { item ->
                    FilterChip(
                        selected = category == item,
                        onClick = { category = item },
                        label = { Text("${item.symbol}  ${item.label}") },
                    )
                }
            }
        }

        if (browsingAll) {
            nearbyEvents.firstOrNull { it.featured }?.let { featured ->
                item {
                    PoiSectionHeader("Featured near you")
                }
                item {
                    PoiEventCard(
                        event = featured,
                        status = attendance[featured.id] ?: AttendanceStatus.NONE,
                        onClick = { onEventClick(featured.id) },
                        featured = true,
                    )
                }
            }

            val live = nearbyEvents.filter { it.isLive(now) && !it.featured }
            if (live.isNotEmpty()) {
                item { PoiSectionHeader("Happening now") }
                items(live, key = { it.id }) { event ->
                    PoiEventCard(
                        event = event,
                        status = attendance[event.id] ?: AttendanceStatus.NONE,
                        onClick = { onEventClick(event.id) },
                    )
                }
            }

            item { PoiSectionHeader("Coming up") }
            items(nearbyEvents.filter { it.startsAtMillis > now && !it.featured }, key = { it.id }) { event ->
                PoiEventCard(
                    event = event,
                    status = attendance[event.id] ?: AttendanceStatus.NONE,
                    onClick = { onEventClick(event.id) },
                )
            }
        } else {
            item {
                PoiSectionHeader("${filtered.size} matching event${if (filtered.size == 1) "" else "s"}")
            }
            if (filtered.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp)) {
                        Text("Nothing found nearby", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Try another category or a broader search.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { event ->
                    PoiEventCard(
                        event = event,
                        status = attendance[event.id] ?: AttendanceStatus.NONE,
                        onClick = { onEventClick(event.id) },
                    )
                }
            }
        }
    }

    if (showRadiusPicker) {
        AlertDialog(
            onDismissRequest = { showRadiusPicker = false },
            title = { Text("Choose your discovery distance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Poi will show events within this distance from your selected area.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    listOf<Int?>(5, 10, 25, 50, 100, null).forEach { radius ->
                        FilterChip(
                            selected = settings.discoveryRadiusKm == radius,
                            onClick = {
                                scope.launch {
                                    repository.updateSettings(settings.copy(discoveryRadiusKm = radius))
                                }
                                showRadiusPicker = false
                            },
                            label = { Text(radius?.let { "Within $it km" } ?: "Any distance") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRadiusPicker = false }) { Text("Close") }
            },
        )
    }
}
