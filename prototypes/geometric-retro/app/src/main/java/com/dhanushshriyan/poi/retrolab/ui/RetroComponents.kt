package com.dhanushshriyan.poi.retrolab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhanushshriyan.poi.retrolab.R
import com.dhanushshriyan.poi.retrolab.data.*
import java.time.format.DateTimeFormatter
import java.util.Locale

val TicketShape = CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)
private val dateFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

fun DemoEvent.dateLabel(): String = date.format(dateFormat)
fun DemoEvent.timeLabel(): String = time.format(timeFormat)
fun categoryColor(category: Category): Color = when (category) {
    Category.DESIGN -> Gold
    Category.MUSIC -> Rust
    Category.MARKET -> Teal
}
fun foreground(background: Color): Color =
    if (background == Teal || background == Rust || background == Ink) Color.White else Ink

@Composable
fun Mono(text: String, modifier: Modifier = Modifier, color: Color = Ink) {
    Text(text, modifier, color = color, fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 15.sp, letterSpacing = 0.3.sp)
}

@Composable
fun Rule(modifier: Modifier = Modifier) = HorizontalDivider(modifier, thickness = 1.dp, color = Ink)

@Composable
fun GeometryMark(modifier: Modifier = Modifier, variant: Int = 0) {
    Canvas(modifier = modifier) {
        val diameter = size.minDimension
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val colors = if (variant % 2 == 0) listOf(Gold, Teal, Cream, Rust) else listOf(Teal, Rust, Gold, Cream)
        colors.forEachIndexed { i, color ->
            drawArc(color, i * 90f, 90f, true, topLeft, Size(diameter, diameter))
        }
        drawCircle(Ink, diameter / 2, style = Stroke(1.2.dp.toPx()))
        drawLine(Ink, Offset(topLeft.x + diameter / 2, topLeft.y),
            Offset(topLeft.x + diameter / 2, topLeft.y + diameter), 1.dp.toPx())
        drawLine(Ink, Offset(topLeft.x, topLeft.y + diameter / 2),
            Offset(topLeft.x + diameter, topLeft.y + diameter / 2), 1.dp.toPx())
    }
}

@Composable
fun RetroPhoto(image: EventImage, description: String?, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(when (image) {
            EventImage.ARCHITECTURE -> R.drawable.retro_architecture
            EventImage.MUSIC -> R.drawable.retro_music
            EventImage.MARKET -> R.drawable.retro_market
        }),
        contentDescription = description, modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}

@Composable
fun RetroAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    color: Color = Gold,
) {
    val fill = if (selected) color else LightCream
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(fill)
            .border(1.dp, Ink)
            .selectable(selected = selected, role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = foreground(fill))
    }
}

@Composable
fun RetroIconButton(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = Ink) {
    IconButton(onClick = onClick) { Icon(icon, label, tint = tint, modifier = Modifier.size(23.dp)) }
}

@Composable
fun RsvpControls(status: Rsvp, onSelect: (Rsvp) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(Rsvp.INTERESTED, Rsvp.GOING, Rsvp.HERE).forEach { choice ->
            RetroAction(
                label = choice.label,
                selected = status == choice,
                color = when (choice) { Rsvp.HERE -> Teal; Rsvp.GOING -> Gold; else -> Gold },
                onClick = { onSelect(choice) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun EventCard(
    event: DemoEvent,
    number: String,
    status: Rsvp,
    onOpen: () -> Unit,
    onRespond: (Rsvp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.clip(TicketShape).background(LightCream)
        .border(BorderStroke(1.2.dp, Ink), TicketShape)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            GeometryMark(Modifier.size(26.dp), event.category.ordinal)
            Spacer(Modifier.width(8.dp))
            Mono("@" + event.host.replace(" ", "_"), Modifier.weight(1f))
            Mono(number)
        }
        Rule()
        Box(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
            RetroPhoto(event.image, event.title, Modifier.fillMaxWidth().aspectRatio(1.46f))
            Box(Modifier.align(Alignment.TopEnd).padding(12.dp)
                .background(categoryColor(event.category)).border(1.dp, Ink).padding(horizontal = 9.dp, vertical = 5.dp)) {
                Mono(event.admission, color = foreground(categoryColor(event.category)))
            }
        }
        Rule()
        Column(Modifier.padding(14.dp)) {
            Mono(event.category.label.uppercase(Locale.ENGLISH) + "  /  " + event.dateLabel().uppercase(Locale.ENGLISH))
            Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(top = 7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text(event.title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, "Event details", Modifier.size(25.dp))
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.LocationOn, null, Modifier.size(17.dp))
                Spacer(Modifier.width(3.dp))
                Text(event.venue + " · " + event.distanceKm + " km", style = MaterialTheme.typography.bodyMedium)
            }
            Text(event.timeLabel() + "  /  " + event.going + " going", color = MutedInk,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 3.dp))
            Spacer(Modifier.height(13.dp))
            RsvpControls(status, onRespond, Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun CompactEvent(event: DemoEvent, status: Rsvp, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(TicketShape).background(LightCream)
        .border(1.dp, Ink, TicketShape).clickable(onClick = onOpen).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RetroPhoto(event.image, null, Modifier.size(76.dp).border(1.dp, Ink))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Mono(event.dateLabel().uppercase(Locale.ENGLISH))
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text(event.timeLabel(), style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            if (status != Rsvp.NONE) Mono(status.label.uppercase(Locale.ENGLISH), color = Teal)
        }
        Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(21.dp))
    }
}

@Composable
fun PageHeading(eyebrow: String, title: String, description: String) {
    Column(Modifier.fillMaxWidth().padding(20.dp)) {
        Mono(eyebrow)
        Spacer(Modifier.height(7.dp))
        Text(title, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(10.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
    }
}

@Composable
fun EmptyPanel(title: String, body: String) {
    Column(Modifier.fillMaxWidth().clip(TicketShape).background(LightCream).border(1.dp, Ink, TicketShape).padding(24.dp)) {
        GeometryMark(Modifier.size(44.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
    }
}
