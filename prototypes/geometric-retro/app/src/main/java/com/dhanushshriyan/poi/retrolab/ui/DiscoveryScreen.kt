package com.dhanushshriyan.poi.retrolab.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhanushshriyan.poi.retrolab.data.*

@Composable
fun DiscoveryScreen(
    state: DemoState,
    searchMode: Boolean,
    query: String,
    category: Category?,
    radius: Int?,
    onQuery: (String) -> Unit,
    onCategory: (Category?) -> Unit,
    onRadius: (Int?) -> Unit,
    onEvent: (String) -> Unit,
) {
    val events = filterEvents(state.events, if (searchMode) query else "", category, radius)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            if (searchMode) {
                Column {
                    PageHeading("02 / SEARCH THE CITY", "Find your\nkind of thing.", "Search sample events, hosts or places. All locations and distances are demo data.")
                    OutlinedTextField(
                        value = query, onValueChange = onQuery,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                        singleLine = true, shape = RectangleShape,
                        placeholder = { Text("Music, markets, design...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) RetroIconButton(Icons.Outlined.Close, "Clear search", { onQuery("") })
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Ink, unfocusedBorderColor = Ink,
                            focusedContainerColor = LightCream, unfocusedContainerColor = LightCream,
                        ),
                    )
                }
            } else {
                GeoBanner()
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Mono("MANGALURU / SAMPLE LOCATIONS")
                        Text("A little closer to your next plan.", style = MaterialTheme.typography.bodyMedium)
                    }
                    RadiusPicker(radius, onRadius)
                }
                LazyRow(contentPadding = PaddingValues(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    item { RetroAction("All", { onCategory(null) }, selected = category == null) }
                    items(Category.entries) { choice ->
                        RetroAction(choice.label, { onCategory(choice) }, selected = category == choice)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(if (searchMode) "SEARCH RESULTS" else "THE LOCAL EDIT",
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Mono(events.size.toString().padStart(2, '0') + " EVENTS")
            }
        }
        if (events.isEmpty()) {
            item { Box(Modifier.padding(horizontal = 18.dp)) {
                EmptyPanel("No plans in this frame.", "Try a different category, a wider radius or another search.")
            } }
        }
        items(events, key = DemoEvent::id) { event ->
            EventCard(
                event, (state.events.indexOf(event) + 1).toString().padStart(2, '0'),
                state.rsvps[event.id] ?: Rsvp.NONE, { onEvent(event.id) },
                { state.respond(event.id, it) }, Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            )
        }
        item {
            Mono("A THEME STUDY. NOT REAL EVENT LISTINGS.",
                Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = MutedInk)
        }
    }
}

@Composable
private fun GeoBanner() {
    Column {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Column(Modifier.weight(0.66f).background(Gold).padding(20.dp)) {
                Mono("EDITION NO. 02 / GEOMETRIC RETRO")
                Spacer(Modifier.height(12.dp))
                Text("FIND YOUR\nNEXT GOOD\nTHING.",
                    fontSize = 31.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
                Spacer(Modifier.height(12.dp))
                Text("Events. People. Possibilities.", style = MaterialTheme.typography.bodyMedium)
            }
            Column(Modifier.weight(0.34f).fillMaxHeight().border(1.dp, Ink)) {
                Canvas(Modifier.fillMaxWidth().weight(1f).background(Teal)) {
                    val d = size.minDimension * .74f
                    val start = Offset((size.width - d) / 2, (size.height - d) / 2)
                    drawArc(Cream, 0f, 180f, true, start, Size(d, d))
                    drawArc(Gold, 180f, 180f, true, start, Size(d, d))
                    drawCircle(Ink, d / 2, style = Stroke(1.5.dp.toPx()))
                    drawLine(Ink, Offset(start.x, size.height / 2), Offset(start.x + d, size.height / 2), 1.dp.toPx())
                }
                Rule()
                Box(Modifier.fillMaxWidth().weight(1f).background(Rust).padding(14.dp), contentAlignment = Alignment.CenterStart) {
                    Text("OUTSIDE\nTHE\nORDINARY.", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, lineHeight = 18.sp)
                }
            }
        }
        Rule()
        Row(Modifier.fillMaxWidth().background(LightCream).padding(horizontal = 18.dp, vertical = 7.dp)) {
            Mono("OFFLINE THEME DEMO", Modifier.weight(1f))
            Mono("NOT CONNECTED TO POI")
        }
        Rule()
    }
}

@Composable
private fun RadiusPicker(radius: Int?, onRadius: (Int?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Surface(onClick = { expanded = true }, color = Cream, shape = RectangleShape,
            border = androidx.compose.foundation.BorderStroke(1.dp, Ink)) {
            Row(Modifier.heightIn(min = 48.dp).padding(start = 10.dp, end = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Mono(radius?.let { "$it KM" } ?: "ALL KM")
                Icon(Icons.Outlined.ExpandMore, "Change sample distance", Modifier.size(21.dp))
            }
        }
        DropdownMenu(expanded, { expanded = false }, containerColor = LightCream) {
            listOf<Int?>(5, 10, 25, null).forEach { distance ->
                DropdownMenuItem(
                    text = { Text(distance?.let { "Within $it km" } ?: "Any distance") },
                    onClick = { onRadius(distance); expanded = false },
                )
            }
        }
    }
}
