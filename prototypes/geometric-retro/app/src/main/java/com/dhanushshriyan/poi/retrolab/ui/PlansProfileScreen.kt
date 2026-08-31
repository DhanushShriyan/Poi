package com.dhanushshriyan.poi.retrolab.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhanushshriyan.poi.retrolab.data.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PlansScreen(state: DemoState, onEvent: (String) -> Unit) {
    var myPlans by rememberSaveable { mutableStateOf(false) }
    var selectedDay by rememberSaveable { mutableLongStateOf(state.today.toEpochDay()) }
    val date = LocalDate.ofEpochDay(selectedDay)
    val events = if (myPlans) state.events.filter { (state.rsvps[it.id] ?: Rsvp.NONE) != Rsvp.NONE }
        else filterEvents(state.events, date = date)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { PageHeading("03 / THE AGENDA", "Make room\nfor good times.", "See what is happening by date, or keep the plans you have marked Interested, Going or Here.") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RetroAction("By date", { myPlans = false }, Modifier.weight(1f), selected = !myPlans)
                RetroAction("My plans", { myPlans = true }, Modifier.weight(1f), selected = myPlans)
            }
        }
        if (!myPlans) {
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items((0L..4L).map { state.today.plusDays(it) }) { pageDate ->
                        val selected = date == pageDate
                        val fill = if (selected) Teal else LightCream
                        Column(Modifier.width(78.dp).background(fill).border(1.dp, Ink)
                            .clickable { selectedDay = pageDate.toEpochDay() }.padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Mono(pageDate.format(DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)).uppercase(), color = foreground(fill))
                            Text(pageDate.dayOfMonth.toString(), color = foreground(fill),
                                fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.Black)
                            Mono(pageDate.format(DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)).uppercase(), color = foreground(fill))
                            Spacer(Modifier.height(5.dp))
                            Text(state.events.count { it.date == pageDate }.toString() + " events",
                                style = MaterialTheme.typography.bodySmall, color = foreground(fill))
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                Mono(if (myPlans) "YOUR SAVED PLANS" else date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.ENGLISH)).uppercase(),
                    Modifier.weight(1f))
                Mono(events.size.toString().padStart(2, '0'))
            }
        }
        if (events.isEmpty()) item {
            Box(Modifier.padding(horizontal = 18.dp)) {
                EmptyPanel(if (myPlans) "Your agenda is open." else "A little breathing room.",
                    if (myPlans) "Mark an event in the feed to see it here. Your demo choices stay on this phone."
                    else "There are no sample events on this date. Try another day.")
            }
        }
        items(events, key = DemoEvent::id) { event ->
            Box(Modifier.padding(horizontal = 18.dp)) {
                CompactEvent(event, state.rsvps[event.id] ?: Rsvp.NONE, { onEvent(event.id) })
            }
        }
    }
}

@Composable
fun ProfileScreen(state: DemoState) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Row(Modifier.fillMaxWidth().background(Gold).padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                    GeometryMark(Modifier.size(56.dp), 1)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Mono("05 / LOCAL TEST PROFILE")
                    Text("The explorer.", style = MaterialTheme.typography.headlineMedium)
                    Text("@you_in_retro", style = MaterialTheme.typography.bodyMedium)
                }
            }
            Rule()
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                ProfileStat(state.rsvps.count { it.value != Rsvp.NONE }.toString(), "PLANS", Gold, Modifier.weight(1f))
                ProfileStat(state.likes.count { it.value }.toString(), "LIKES", Teal, Modifier.weight(1f))
                ProfileStat(state.comments.values.sumOf { it.size }.toString(), "COMMENTS", Rust, Modifier.weight(1f))
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Mono("THE VISUAL SYSTEM")
                Spacer(Modifier.height(8.dp))
                Text("Less decoration.\nMore character.", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                Text("Cream canvas, bold geometry, crisp rules and warm editorial photos. A new look for the same idea: finding good things nearby.",
                    style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
                listOf("CREAM" to Cream, "SUN" to Gold, "TIDE" to Teal, "CLAY" to Rust).forEach { (label, color) ->
                    Box(Modifier.weight(1f).height(76.dp).background(color).border(1.dp, Ink), contentAlignment = Alignment.BottomStart) {
                        Mono(label, Modifier.padding(8.dp), foreground(color))
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("What do you think?", style = MaterialTheme.typography.titleLarge)
                listOf(
                    "01" to "Is this easier to scan than the scribble theme?",
                    "02" to "Do the coloured blocks feel warm or too busy?",
                    "03" to "Which looks better: event cards or the moments feed?",
                    "04" to "Would you want this direction in the main Poi app?",
                ).forEach { (number, question) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Mono(number, Modifier.width(30.dp), Teal)
                        Text(question, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 18.dp).clip(TicketShape)
                .background(Ink).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Mono("A SAFE, SEPARATE EXPERIMENT", color = Gold)
                Text("No login. No live database. No real attendance or bookings. All photos, names and events are fictional demo content.",
                    color = Color.White, style = MaterialTheme.typography.bodyMedium)
                Text("Poi and the Scribble Lab remain separate and unchanged.",
                    color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
@Composable
private fun ProfileStat(value: String, label: String, fill: Color, modifier: Modifier) {
    Column(modifier.background(fill).border(1.dp, Ink).padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = foreground(fill), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Mono(label, color = foreground(fill))
    }
}

