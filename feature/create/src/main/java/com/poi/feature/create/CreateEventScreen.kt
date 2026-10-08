package com.poi.feature.create

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.data.EventRepository
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.location.LocationRepository
import com.poi.core.model.EventCategory
import com.poi.core.model.EventVisibility
import com.poi.core.model.NewEvent
import com.poi.core.poster.eventTimestamp
import com.poi.core.poster.CloudPosterReader
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Composable
fun CreateEventScreen(
    repository: EventRepository,
    organizerName: String,
    locationRepository: LocationRepository,
    onCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    cloudPosterReader: CloudPosterReader? = null,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var summary by rememberSaveable { mutableStateOf("") }
    var venue by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var category by remember { mutableStateOf(EventCategory.FESTIVAL) }
    var visibility by remember { mutableStateOf(EventVisibility.PUBLIC) }
    var startDate by rememberSaveable { mutableStateOf("") }
    var startTime by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var endTime by rememberSaveable { mutableStateOf("") }
    var accepted by remember { mutableStateOf(false) }
    var showErrors by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var publishError by remember { mutableStateOf<String?>(null) }
    var attachCoordinates by remember { mutableStateOf(false) }
    var checkInRadiusMeters by remember { mutableStateOf(500) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationState by locationRepository.state.collectAsStateWithLifecycle()
    val hasLocationPermission = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }
    val loadVenueLocation: () -> Unit = {
        scope.launch {
            locationRepository.refresh().onSuccess { attachCoordinates = true }
        }
    }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.any { it }) loadVenueLocation()
    }
    val chooseVenueLocation: () -> Unit = {
        if (hasLocationPermission()) {
            loadVenueLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }
    val startsAt = eventTimestamp(startDate, startTime)
    val endsAt = eventTimestamp(endDate, endTime)
    val validSchedule = startsAt != null && endsAt != null && endsAt > startsAt
    val valid = title.isNotBlank() && summary.isNotBlank() && venue.isNotBlank() && address.isNotBlank() && accepted && validSchedule

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
            PosterImportCard(enabled = !saving, cloudReader = cloudPosterReader) { draft ->
                title = draft.title; summary = draft.summary; venue = draft.venue; address = draft.address
                category = draft.category
                startDate = draft.date.orEmpty(); startTime = draft.time.orEmpty()
                endDate = draft.endDate.orEmpty(); endTime = draft.endTime.orEmpty()
                accepted = false; showErrors = false; publishError = null; attachCoordinates = false
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
            EventDateTimeField("Start date", startDate, true, { startDate = it })
            EventDateTimeField("Start time", startTime, false, { startTime = it })
            EventDateTimeField("End date", endDate, true, { endDate = it })
            EventDateTimeField("End time", endTime, false, { endTime = it })
            Spacer(Modifier.height(6.dp))
            Text(
                if (showErrors && !validSchedule) "Choose valid start and end details. End must be after start."
                else "Confirm dates and times against the poster. Missing years and durations are never filled automatically.",
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
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = chooseVenueLocation,
                modifier = Modifier.fillMaxWidth(),
                enabled = !locationState.isLoading,
            ) {
                Icon(Icons.Default.MyLocation, null)
                Spacer(Modifier.padding(4.dp))
                Text(
                    when {
                        locationState.isLoading -> "Finding venue location…"
                        attachCoordinates -> "Venue location attached"
                        else -> "Use my current location for this venue"
                    },
                )
            }
            Text(
                when {
                    attachCoordinates -> "Only the venue point is published. Your live device location is not retained."
                    locationState.errorMessage != null -> locationState.errorMessage.orEmpty()
                    else -> "Adding a venue point enables accurate distance and proximity check-in."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (locationState.errorMessage != null && !attachCoordinates) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            if (attachCoordinates) {
                Spacer(Modifier.height(10.dp))
                Text("Check-in area", style = MaterialTheme.typography.titleMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf(250, 500, 1_000, 2_500)) { radius ->
                        FilterChip(
                            selected = checkInRadiusMeters == radius,
                            onClick = { checkInRadiusMeters = radius },
                            label = { Text(if (radius < 1_000) "$radius m" else "${radius / 1_000.0} km") },
                        )
                    }
                }
            }
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
                    publishError = null
                    val starts = checkNotNull(startsAt)
                    scope.launch {
                        runCatching {
                            repository.createEvent(
                                NewEvent(
                                    title = title,
                                    summary = summary,
                                    category = category,
                                    startsAtMillis = starts,
                                    endsAtMillis = checkNotNull(endsAt),
                                    venue = venue,
                                    address = address,
                                    visibility = visibility,
                                    organizerName = organizerName,
                                    latitude = locationState.snapshot?.point?.latitude.takeIf { attachCoordinates },
                                    longitude = locationState.snapshot?.point?.longitude.takeIf { attachCoordinates },
                                    checkInRadiusMeters = checkInRadiusMeters,
                                ),
                            )
                        }.onSuccess { event ->
                            onCreated(event.id)
                        }.onFailure { throwable ->
                            publishError = createEventErrorMessage(throwable)
                        }
                        saving = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !saving,
            ) {
                Icon(Icons.Default.CheckCircle, null)
                Spacer(Modifier.padding(4.dp))
                Text(if (saving) "Publishing…" else "Publish for review")
            }
            publishError?.let { message ->
                Spacer(Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        message,
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun EventDateTimeField(label: String, value: String, date: Boolean, onValue: (String) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        onClick = {
            if (date) {
                val initial = runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }
                DatePickerDialog(context, { _, year, month, day -> onValue(LocalDate.of(year, month + 1, day).toString()) },
                    initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
            } else {
                val initial = runCatching { LocalTime.parse(value) }.getOrElse { LocalTime.of(9, 0) }
                TimePickerDialog(context, { _, hour, minute -> onValue(LocalTime.of(hour, minute).toString()) },
                    initial.hour, initial.minute, true).show()
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value.ifBlank { "Choose ${if (date) "date" else "time"}" }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

internal fun createEventErrorMessage(error: Throwable): String {
    val message = error.message.orEmpty()
    val normalized = message.lowercase()
    return when {
        normalized.contains("unable to resolve host") ||
            normalized.contains("no address associated with hostname") ||
            normalized.contains("failed to connect") ||
            normalized.contains("timeout") ->
            "Poi could not reach the event service. Check your internet connection and try again."
        normalized.contains("sign in") || normalized.contains("jwt") ->
            "Your session has expired. Sign in again, then publish the event."
        normalized.contains("row-level security") || normalized.contains("permission denied") ->
            "This account cannot publish the event yet. Sign out, sign in, and retry."
        else -> "We couldn't publish this event. Please review the details and try again."
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
        shape = MaterialTheme.shapes.medium,
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
