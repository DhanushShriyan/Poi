package com.poi.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventTest {
    private val event = Event(
        id = "test",
        title = "Test event",
        summary = "Summary",
        description = "Description",
        category = EventCategory.COMMUNITY,
        startsAtMillis = 1_000,
        endsAtMillis = 2_000,
        venue = "Venue",
        address = "Address",
        distanceKm = 1.0,
        organizer = Organizer("Organizer", true),
        visibility = EventVisibility.PUBLIC,
        verification = VerificationLevel.CONFIRMED,
        attendeeCount = 10,
        friendNames = emptyList(),
        themeKey = "sunset",
    )

    @Test fun liveWindowIncludesStartAndEnd() {
        assertTrue(event.isLive(1_000))
        assertTrue(event.isLive(1_500))
        assertTrue(event.isLive(2_000))
        assertFalse(event.isLive(999))
        assertFalse(event.isLive(2_001))
    }
}

