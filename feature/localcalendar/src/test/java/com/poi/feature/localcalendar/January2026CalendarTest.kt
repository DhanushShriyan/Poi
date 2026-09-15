package com.poi.feature.localcalendar

import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class January2026CalendarTest {
    @Test
    fun `contains every January day and its daily sun times`() {
        assertEquals(YearMonth.of(2026, 1), January2026Calendar.month)
        assertEquals(31, January2026Calendar.days.size)
        assertEquals(LocalTime.of(6, 54), January2026Calendar.day(1).sunrise)
        assertEquals(LocalTime.of(18, 14), January2026Calendar.day(1).sunset)
        assertEquals(LocalTime.of(6, 58), January2026Calendar.day(31).sunrise)
        assertEquals(LocalTime.of(18, 30), January2026Calendar.day(31).sunset)
    }

    @Test
    fun `preserves empty days instead of inventing observances`() {
        assertTrue(January2026Calendar.day(7).observances.isEmpty())
        assertTrue(January2026Calendar.day(13).observances.isEmpty())
    }

    @Test
    fun `contains the three calendar highlights`() {
        val makar = January2026Calendar.day(14).observances.single {
            it.title == "ಮಕರಸಂಕ್ರಮಣ"
        }
        val kodialTeru = January2026Calendar.day(25).observances.single {
            it.title == "Kodial Teru"
        }
        val republicDay = January2026Calendar.day(26).observances.single {
            it.title == "ಗಣರಾಜ್ಯ ದಿನ"
        }

        assertTrue(makar.featured)
        assertTrue(kodialTeru.featured)
        assertTrue(republicDay.featured)
        assertEquals(LocalObservanceKind.CIVIC, republicDay.kind)
    }

    @Test
    fun `ships the complete supplied month rather than a small demo sample`() {
        val printedEntries = January2026Calendar.days.sumOf { it.observances.size }
        val populatedDates = January2026Calendar.days.count { it.observances.isNotEmpty() }

        assertTrue(printedEntries >= 90)
        assertEquals(29, populatedDates)
        assertEquals("Sharada Calendar 2026", January2026Calendar.SOURCE)
    }

    @Test
    fun `English is the default calendar language`() {
        assertEquals(LocalCalendarLanguage.ENGLISH, LocalCalendarLanguage.DEFAULT)
        assertEquals(LocalCalendarLanguage.ENGLISH, LocalCalendarLanguage.fromStorage(null))
        assertEquals(LocalCalendarLanguage.KANNADA, LocalCalendarLanguage.fromStorage("kn"))
    }

    @Test
    fun `every Kannada source record has an English display title`() {
        val untranslated = January2026Calendar.days
            .flatMap { it.observances }
            .filter { observance ->
                observance.title.any { it in '\u0C80'..'\u0CFF' } &&
                    observance.displayTitle(LocalCalendarLanguage.ENGLISH) == observance.title
            }

        assertTrue("Missing English titles: ${untranslated.map { it.title }}", untranslated.isEmpty())
        assertEquals(
            "Makara Sankramana",
            January2026Calendar.day(14).observances.first { it.featured }
                .displayTitle(LocalCalendarLanguage.ENGLISH),
        )
        assertEquals(
            "Republic Day",
            January2026Calendar.day(26).observances.first { it.featured }
                .displayTitle(LocalCalendarLanguage.ENGLISH),
        )
    }
}
