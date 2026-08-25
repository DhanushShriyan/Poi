package com.poi.core.data

import com.poi.core.model.AttendanceStatus
import com.poi.core.model.Event
import com.poi.core.model.EventCategory

fun List<Event>.personalizedFor(
    attendance: Map<String, AttendanceStatus>,
    friendEventIds: Set<String>,
    nowMillis: Long = System.currentTimeMillis(),
    limit: Int = 3,
): List<Event> {
    val categoryAffinity = asSequence()
        .filter { attendance[it.id] in setOf(AttendanceStatus.INTERESTED, AttendanceStatus.GOING, AttendanceStatus.HERE, AttendanceStatus.ATTENDED) }
        .groupingBy(Event::category)
        .eachCount()

    return asSequence()
        .filter { it.endsAtMillis >= nowMillis }
        .map { event ->
            val score = (categoryAffinity[event.category] ?: 0) * 25 +
                (if (event.id in friendEventIds) 60 else 0) +
                (if (event.featured) 15 else 0) +
                (if (event.distanceKm <= 10.0) 10 else 0) +
                (if (event.category == EventCategory.PRIVATE) -20 else 0)
            event to score
        }
        .sortedWith(compareByDescending<Pair<Event, Int>> { it.second }.thenBy { it.first.startsAtMillis })
        .map(Pair<Event, Int>::first)
        .take(limit.coerceAtLeast(0))
        .toList()
}
