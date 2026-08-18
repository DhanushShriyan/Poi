package com.poi.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.poi.core.model.AttendanceStatus
import com.poi.core.model.Event
import com.poi.core.model.EventVisibility
import com.poi.core.model.VerificationLevel
import com.poi.core.model.isLive
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun PoiSectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
fun PoiStatusPill(
    text: String,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun PoiInitialAvatar(
    name: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.primaryContainer,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifBlank { "P" },
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun PoiEventArtwork(
    event: Event,
    modifier: Modifier = Modifier,
) {
    val colors = artworkColors(event.themeKey)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(colors)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 18.dp, end = 18.dp)
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
        )
        Text(
            text = event.category.symbol,
            style = MaterialTheme.typography.displaySmall,
            color = Color.White,
            modifier = Modifier.align(Alignment.Center),
        )
        if (event.isLive(System.currentTimeMillis())) {
            PoiStatusPill(
                text = "● LIVE",
                containerColor = Color(0xFFE33B3B),
                contentColor = Color.White,
            )
        }
    }
}

@Composable
fun PoiEventCard(
    event: Event,
    status: AttendanceStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    featured: Boolean = false,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        if (featured) {
            PoiEventArtwork(event, Modifier.fillMaxWidth().height(178.dp))
        }
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatEventDate(event.startsAtMillis),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelLarge,
                )
                if (event.visibility != EventVisibility.PUBLIC) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Private event",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (status != AttendanceStatus.NONE) {
                    PoiStatusPill(status.label)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${event.venue}  ·  ${formatDistance(event.distanceKm)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (event.verification >= VerificationLevel.ORGANIZER) {
                        Icons.Default.CheckCircle
                    } else {
                        Icons.Default.Group
                    },
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = if (event.verification >= VerificationLevel.ORGANIZER) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = event.verification.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${event.attendeeCount} going",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (event.friendNames.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                val visibleFriends = event.friendNames.take(2).joinToString(" & ")
                val extraFriends = if (event.friendNames.size > 2) " +${event.friendNames.size - 2}" else ""
                Text(
                    text = "$visibleFriends$extraFriends are interested",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun PoiSettingRow(
    icon: ImageVector,
    title: String,
    supporting: String,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

fun formatEventDate(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val today = java.time.LocalDate.now()
    return when (date.toLocalDate()) {
        today -> "TODAY · ${date.format(DateTimeFormatter.ofPattern("h:mm a"))}"
        today.plusDays(1) -> "TOMORROW · ${date.format(DateTimeFormatter.ofPattern("h:mm a"))}"
        else -> date.format(DateTimeFormatter.ofPattern("EEE, d MMM · h:mm a")).uppercase()
    }
}

fun formatEventTime(millis: Long): String = Instant.ofEpochMilli(millis)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("h:mm a"))

private fun formatDistance(distanceKm: Double): String = when {
    distanceKm <= 0.0 -> "Your event"
    distanceKm < 1.0 -> "${(distanceKm * 1000).toInt()} m"
    else -> "${"%.1f".format(distanceKm)} km"
}

private fun artworkColors(key: String): List<Color> = when (key) {
    "festival" -> listOf(Color(0xFFF16F54), Color(0xFFF4B942))
    "concert" -> listOf(Color(0xFF472A79), Color(0xFFB34D8C))
    "sale" -> listOf(Color(0xFFCB365D), Color(0xFFF28D52))
    "community" -> listOf(Color(0xFF147A62), Color(0xFF4AA89A))
    "sports" -> listOf(Color(0xFF18599B), Color(0xFF22A3B8))
    "workshop" -> listOf(Color(0xFF99633A), Color(0xFFD0925D))
    "private" -> listOf(Color(0xFF6D526F), Color(0xFFA97982))
    else -> listOf(PoiGreen, PoiCoral)
}
