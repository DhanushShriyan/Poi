package com.poi.core.poster

import com.poi.core.model.EventCategory
import org.junit.Assert.*
import org.junit.Test

class PosterParserTest {
    @Test fun englishPoster() {
        val result = PosterParser.parse("Event name: Coastal Music Night\nDate: 18 October 2026\nTime: 6:30 PM\nVenue: City Hall\nAddress: Mangaluru")
        assertEquals("Coastal Music Night", result.title)
        assertEquals("2026-10-18", result.date)
        assertEquals("18:30", result.time)
        assertEquals("City Hall", result.venue)
        assertEquals("Mangaluru", result.address)
        assertEquals(EventCategory.CONCERT, result.category)
        assertNull(result.endTime)
    }
    @Test fun kannadaPosterAndDigits() {
        val result = PosterParser.parse("ಕಾರ್ಯಕ್ರಮದ ಹೆಸರು: ವಾರ್ಷಿಕ ಉತ್ಸವ\nದಿನಾಂಕ: ೧೮ ಅಕ್ಟೋಬರ್ ೨೦೨೬\nಸಂಜೆ ೬:೩೦\nಸ್ಥಳ: ದೇವಸ್ಥಾನ ಸಭಾಂಗಣ\nವಿಳಾಸ: ಮಂಗಳೂರು")
        assertEquals("2026-10-18", result.date)
        assertEquals("18:30", result.time)
        assertEquals("ದೇವಸ್ಥಾನ ಸಭಾಂಗಣ", result.venue)
        assertEquals(EventCategory.FESTIVAL, result.category)
    }
    @Test fun hindiPosterAndDigits() {
        val result = PosterParser.parse("कार्यक्रम का नाम: संगीत महोत्सव\n१८ अक्टूबर २०२६\nशाम ७:३०\nस्थान: नगर भवन\nपता: उडुपी")
        assertEquals("2026-10-18", result.date)
        assertEquals("19:30", result.time)
        assertEquals("नगर भवन", result.venue)
    }
    @Test fun noInventedYearOrTime() {
        val result = PosterParser.parse("Local festival\n18 October\n6:30\nCall 9876543210")
        assertNull(result.date); assertNull(result.time); assertEquals("", result.venue)
    }
    @Test fun ambiguousDatesNeedReview() {
        assertNull(PosterParser.parse("Festival\n18/10/2026\n19/10/2026").date)
        assertNull(PosterParser.parse("Festival\n18-20 October 2026").date)
    }
    @Test fun invalidDateNotNormalised() {
        assertNull(PosterParser.parse("Festival\n31/02/2026").date)
    }
    @Test fun numericDateNotMistakenForRange() {
        assertEquals("2026-10-08", PosterParser.parse("Festival\n08-10-2026").date)
        assertEquals("2026-10-08", PosterParser.parse("Festival\n2026-10-08").date)
    }
    @Test fun multipleTimesNeedReviewAndMidnightCorrect() {
        assertNull(PosterParser.parse("Festival\n6 PM\n8 PM").time)
        assertEquals("00:00", PosterParser.parse("Festival\n12 AM").time)
        assertEquals("12:00", PosterParser.parse("Festival\n12 PM").time)
    }
    @Test fun timestampRequiresBothFields() {
        assertNull(eventTimestamp("2026-10-18", ""))
        assertNotNull(eventTimestamp("2026-10-18", "18:30"))
    }
}
