package com.poi.feature.plans

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.data.EventRepository
import com.poi.core.designsystem.PoiEventCard
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.model.AttendanceStatus

@Composable
fun PlansScreen(
    repository: EventRepository,
    onEventClick: (String) -> Unit,
    onDiscover: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val events by repository.events.collectAsStateWithLifecycle()
    val attendance by repository.attendance.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    val now = System.currentTimeMillis()
    val planned = events.filter { attendance[it.id] in setOf(
        AttendanceStatus.INTERESTED,
        AttendanceStatus.GOING,
        AttendanceStatus.HERE,
    ) }
    val upcoming = planned.filter { it.endsAtMillis >= now }.sortedBy { it.startsAtMillis }
    val memories = events.filter {
        attendance[it.id] == AttendanceStatus.ATTENDED || it.endsAtMillis < now && attendance[it.id] != null
    }.sortedByDescending { it.startsAtMillis }
    val visible = if (selectedTab == 0) upcoming else memories

    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp)) {
            Text("Your plans", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "Everything you saved, in one calm place.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Upcoming (${upcoming.size})") },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Memories (${memories.size})") },
            )
        }

        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (selectedTab == 0) Icons.Default.CalendarMonth else Icons.Default.Explore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        if (selectedTab == 0) "No plans yet" else "Your memories will appear here",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (selectedTab == 0) "Mark an event as interested or going to keep it here."
                        else "Events you check into become part of your private event history.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (selectedTab == 0) {
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = onDiscover) { Text("Discover events") }
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 112.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (selectedTab == 0) {
                    val going = visible.filter { attendance[it.id] in setOf(AttendanceStatus.GOING, AttendanceStatus.HERE) }
                    val interested = visible.filter { attendance[it.id] == AttendanceStatus.INTERESTED }
                    if (going.isNotEmpty()) {
                        item { PoiSectionHeader("Going") }
                        items(going, key = { "going-${it.id}" }) { event ->
                            PoiEventCard(
                                event,
                                attendance[event.id] ?: AttendanceStatus.NONE,
                                onClick = { onEventClick(event.id) },
                            )
                        }
                    }
                    if (interested.isNotEmpty()) {
                        item { PoiSectionHeader("Interested") }
                        items(interested, key = { "interested-${it.id}" }) { event ->
                            PoiEventCard(
                                event,
                                AttendanceStatus.INTERESTED,
                                onClick = { onEventClick(event.id) },
                            )
                        }
                    }
                } else {
                    items(visible, key = { it.id }) { event ->
                        PoiEventCard(
                            event,
                            attendance[event.id] ?: AttendanceStatus.ATTENDED,
                            onClick = { onEventClick(event.id) },
                        )
                    }
                }
            }
        }
    }
}

