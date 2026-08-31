package com.dhanushshriyan.poi.retrolab.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DemoCatalogTest {
    private val today = LocalDate.of(2026, 8, 31)
    private val events = demoCatalog(today)

    @Test fun datesRollIntoNextMonthWithoutStaleFixedDates() {
        assertEquals(LocalDate.of(2026, 9, 1), events.first { it.id == "made-local" }.date)
        assertEquals(today, events.first().date)
    }
    @Test fun allCatalogEventsAreVisibleWithNoFilters() {
        assertEquals(5, filterEvents(events).size)
    }
    @Test fun searchIgnoresCaseAndOuterWhitespace() {
        assertEquals(listOf("analog-hours"), filterEvents(events, query = "  aNaLoG ").map { it.id })
    }
    @Test fun searchIncludesOrganizer() {
        assertEquals(2, filterEvents(events, query = "form collective").size)
    }
    @Test fun radiusAndCategoryBothApply() {
        assertTrue(filterEvents(events, category = Category.MARKET, maxDistanceKm = 5).isEmpty())
        assertEquals(listOf("made-local"), filterEvents(events, category = Category.MARKET, maxDistanceKm = 8).map { it.id })
    }
    @Test fun calendarDateIsExact() {
        assertEquals(listOf("form-house", "analog-hours"), filterEvents(events, date = today).map { it.id })
    }
    @Test fun choosingTheSameResponseUnsetsIt() {
        assertEquals(Rsvp.NONE, toggleRsvp(Rsvp.GOING, Rsvp.GOING))
        assertEquals(Rsvp.HERE, toggleRsvp(Rsvp.GOING, Rsvp.HERE))
    }
    @Test fun commentsRejectEmptyAndOverLimitValues() {
        assertNull(normalizeComment("  "))
        assertNull(normalizeComment("x".repeat(281)))
        assertEquals("Looks good", normalizeComment("  Looks good  "))
        assertNotNull(normalizeComment("x".repeat(280)))
    }
}

