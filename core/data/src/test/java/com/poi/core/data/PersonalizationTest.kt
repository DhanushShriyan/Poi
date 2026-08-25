package com.poi.core.data

import com.poi.core.model.AttendanceStatus
import com.poi.core.model.Event
import com.poi.core.model.EventCategory
import com.poi.core.model.EventVisibility
import com.poi.core.model.Organizer
import com.poi.core.model.VerificationLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalizationTest {
    @Test
    fun `friend activity ranks an event ahead of a plain event`() {
        val now = 1_000L
        val plain = event("plain", EventCategory.SALE, now + 1_000)
        val friendPlan = event("friend", EventCategory.COMMUNITY, now + 2_000)

        val result = listOf(plain, friendPlan).personalizedFor(
            attendance = emptyMap(),
            friendEventIds = setOf("friend"),
            nowMillis = now,
        )

        assertEquals("friend", result.first().id)
    }

    @Test
    fun `past events are excluded from recommendations`() {
        val result = listOf(event("past", EventCategory.CONCERT, 100, endsAt = 200))
            .personalizedFor(emptyMap(), emptySet(), nowMillis = 500)

        assertEquals(emptyList<Event>(), result)
    }

    private fun event(
        id: String,
        category: EventCategory,
        startsAt: Long,
        endsAt: Long = startsAt + 1_000,
    ) = Event(
        id = id,
        title = id,
        summary = id,
        description = id,
        category = category,
        startsAtMillis = startsAt,
        endsAtMillis = endsAt,
        venue = "Venue",
        address = "Address",
        distanceKm = 2.0,
        organizer = Organizer("Organizer", false),
        visibility = EventVisibility.PUBLIC,
        verification = VerificationLevel.COMMUNITY,
        attendeeCount = 0,
        friendNames = emptyList(),
        themeKey = "community",
    )
}
