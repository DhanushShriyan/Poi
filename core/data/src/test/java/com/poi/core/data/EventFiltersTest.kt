package com.poi.core.data

import com.poi.core.model.Event
import com.poi.core.model.EventCategory
import com.poi.core.model.EventVisibility
import com.poi.core.model.Organizer
import com.poi.core.model.VerificationLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class EventFiltersTest {
    private val events = listOf(
        event("1", "Village Festival", EventCategory.FESTIVAL, "Kadri"),
        event("2", "Weekend Sale", EventCategory.SALE, "Car Street"),
    )

    @Test fun filtersByCategory() {
        assertEquals(listOf("1"), events.searchAndFilter("", EventCategory.FESTIVAL).map { it.id })
    }

    @Test fun searchesVenueCaseInsensitively() {
        assertEquals(listOf("2"), events.searchAndFilter("CAR", EventCategory.ALL).map { it.id })
    }

    private fun event(id: String, title: String, category: EventCategory, venue: String) = Event(
        id = id,
        title = title,
        summary = "Summary",
        description = "Description",
        category = category,
        startsAtMillis = id.toLong(),
        endsAtMillis = id.toLong() + 1,
        venue = venue,
        address = "Mangaluru",
        distanceKm = 1.0,
        organizer = Organizer("Organizer", true),
        visibility = EventVisibility.PUBLIC,
        verification = VerificationLevel.CONFIRMED,
        attendeeCount = 1,
        friendNames = emptyList(),
        themeKey = "festival",
    )
}

