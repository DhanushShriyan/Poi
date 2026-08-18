package com.poi.core.data

import com.poi.core.model.AppSettings
import com.poi.core.model.AttendanceStatus
import com.poi.core.model.CheckInVisibility
import com.poi.core.model.Event
import com.poi.core.model.EventCategory
import com.poi.core.model.NewEvent
import com.poi.core.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface EventRepository {
    val events: StateFlow<List<Event>>
    val attendance: StateFlow<Map<String, AttendanceStatus>>
    val checkInVisibility: StateFlow<Map<String, CheckInVisibility>>
    val settings: StateFlow<AppSettings>
    val profile: StateFlow<UserProfile>

    suspend fun setAttendance(
        eventId: String,
        status: AttendanceStatus,
        visibility: CheckInVisibility? = null,
    )

    suspend fun createEvent(newEvent: NewEvent): Event
    suspend fun reportEvent(eventId: String, reason: String)
    suspend fun updateSettings(settings: AppSettings)
}

fun List<Event>.searchAndFilter(
    query: String,
    category: EventCategory,
): List<Event> {
    val normalized = query.trim().lowercase()
    return filter { event ->
        val matchesCategory = category == EventCategory.ALL || event.category == category
        val matchesQuery = normalized.isBlank() || listOf(
            event.title,
            event.summary,
            event.venue,
            event.address,
            event.organizer.name,
        ).any { it.lowercase().contains(normalized) }
        matchesCategory && matchesQuery
    }.sortedBy { it.startsAtMillis }
}

