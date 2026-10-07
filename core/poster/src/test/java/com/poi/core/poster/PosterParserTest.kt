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
    // Human transcriptions of the supplied posters test structuring, not native OCR accuracy.
    @Test fun gurupuraSummaryWithoutYear() {
        val result = PosterParser.parse("What's Happening in Mangalore\nGURUPURA KAMBALA\nApr 04 & 05\nManibettu guthu, Gurupura")
        assertEquals("GURUPURA KAMBALA", result.title)
        assertEquals("Manibettu guthu, Gurupura", result.venue)
        assertEquals(listOf(4, 5), result.dateSuggestions.map { it.day })
        assertNull(result.date)
        assertEquals("2026-04-04", result.dateSuggestions.first().resolve(2026))
    }
    @Test fun separateIplDaysNotContinuous() {
        val result = PosterParser.parse("This Weekend in & Around Mangalore\nIPL FAN PARK\nMay 29 & 31\nKaravali Utsav Ground, Mangalore")
        assertEquals("IPL FAN PARK", result.title)
        assertEquals(listOf(29, 31), result.dateSuggestions.map { it.day })
        assertFalse(result.continuousDateRange)
        assertEquals(EventCategory.SPORTS, result.category)
    }
    @Test fun concertWithYearElsewhere() {
        val result = PosterParser.parse("This Weekend in & Around Mangalore\nSAVAARI LIVE CONCERT\nSUNDAY\n19 APRIL\n2026\nApr 19\nKodi Beach, Kundapura")
        assertEquals("SAVAARI LIVE CONCERT", result.title)
        assertEquals("2026-04-19", result.date)
        assertEquals("Kodi Beach, Kundapura", result.venue)
        assertNull(result.time)
    }
    @Test fun bantwalaRange() {
        val result = PosterParser.parse("BANTWALA KAMBALA\nMar 07 - 08\nNavoor, Bantwala")
        assertEquals(listOf(7, 8), result.dateSuggestions.map { it.day })
        assertTrue(result.continuousDateRange)
        assertEquals("Navoor, Bantwala", result.venue)
    }
    @Test fun circusSelectedDaysAndDailyShows() {
        val result = PosterParser.parse("BY HUGE PUBLIC DEMAND WE ARE BACK\nರಾಂಬೋ ಸರ್ಕಸ್\nAUGUST : 22, 23, 26, 27, 28, 29, 30. SEPT.: 4, 5, 6\nDAILY 2 SHOWS\n5.30PM & 8.00PM\nDR TMA PAI INTL. CONVENTION CENTER\nMG ROAD, MANGALURU")
        assertEquals("ರಾಂಬೋ ಸರ್ಕಸ್", result.title)
        assertEquals(10, result.dateSuggestions.size)
        assertEquals(listOf("17:30", "20:00"), result.timeSuggestions)
        assertNull(result.time)
        assertFalse(result.continuousDateRange)
        assertEquals("DR TMA PAI INTL. CONVENTION CENTER", result.venue)
        assertTrue(result.address.contains("MG ROAD"))
    }
    @Test fun historicalKannadaDateRangeStaysHistorical() {
        val result = PosterParser.parse("ಧರ್ಮ ನೇಮ\nದಿನಾಂಕ 23-04-2022 ರಿಂದ 29-04-2022 ವರೆಗೆ")
        assertEquals(listOf("2022-04-23", "2022-04-29"), result.dateSuggestions.map { it.resolve() })
        assertTrue(result.continuousDateRange)
        assertNull(result.date)
    }
    @Test fun geometryRejectsLowConfidenceWords() {
        val hocr = """<span class='ocr_line' title='bbox 0 0 300 12'><span class='ocrx_word' title='x_wconf 90'>Mangalore</span></span>
            <span class='ocr_line' title='bbox 0 50 350 100'><span class='ocrx_word' title='x_wconf 92'>GURUPURA</span><span class='ocrx_word' title='x_wconf 95'>KAMBALA</span><span class='ocrx_word' title='x_wconf 8'>xx#</span></span>"""
        val result = PosterOcrLayout.orderedText(hocr, "junk")
        assertEquals("GURUPURA KAMBALA", result.lines().first())
        assertFalse(result.contains("xx#"))
    }
}
