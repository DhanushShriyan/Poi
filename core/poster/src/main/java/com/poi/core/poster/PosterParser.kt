package com.poi.core.poster

import com.poi.core.model.EventCategory
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/** Conservative, deterministic structuring of OCR text. No invented dates or locations. */
object PosterParser {
    private val monthNames = listOf(
        "january|jan|ಜನವರಿ|जनवरी", "february|feb|ಫೆಬ್ರವರಿ|फरवरी", "march|mar|ಮಾರ್ಚ್|मार्च",
        "april|apr|ಏಪ್ರಿಲ್|अप्रैल", "may|ಮೇ|मई", "june|jun|ಜೂನ್|जून",
        "july|jul|ಜುಲೈ|जुलाई", "august|aug|ಆಗಸ್ಟ್|अगस्त", "september|sept|sep|ಸೆಪ್ಟೆಂಬರ್|सितंबर",
        "october|oct|ಅಕ್ಟೋಬರ್|अक्टूबर", "november|nov|ನವೆಂಬರ್|नवंबर", "december|dec|ಡಿಸೆಂಬರ್|दिसंबर",
    )
    private val monthPattern = monthNames.joinToString("|")
    private val venueLabels = "venue|location|ಸ್ಥಳ|ಸ್ಥಳದಲ್ಲಿ|ಸ್ಥಾನ|स्थान|स्थल"
    private val addressLabels = "address|ವಿಳಾಸ|पता"
    private val titleLabels = "event name|event title|ಕಾರ್ಯಕ್ರಮದ ಹೆಸರು|कार्यक्रम का नाम"

    fun normalizeDigits(text: String): String = text.map { character ->
        if (character.isDigit()) Character.digit(character, 10).takeIf { it >= 0 }?.let { '0' + it } ?: character else character
    }.joinToString("")

    fun parse(raw: String): PosterDraft {
        val text = normalizeDigits(raw).take(20_000).trim()
        val lines = text.lines().map(String::trim).filter(String::isNotBlank)
        val warnings = mutableListOf("Review the suggested name and category against the poster.")
        fun labelled(labels: String): String = lines.firstNotNullOfOrNull { line ->
            Regex("^(?:$labels)\\s*[:：–-]\\s*(.+)$", RegexOption.IGNORE_CASE).find(line)?.groupValues?.get(1)
        }.orEmpty()
        val title = labelled(titleLabels).ifBlank {
            lines.firstOrNull {
                it.length in 4..100 && it.count(Char::isLetter) >= 4 &&
                    !Regex("^(?:$venueLabels|$addressLabels|date|time|contact|phone|ದಿನಾಂಕ|ಸಮಯ|संपर्क|दिनांक|समय)", RegexOption.IGNORE_CASE).containsMatchIn(it) &&
                    !Regex("\\d{4}|https?://|www\\.").containsMatchIn(it)
            }.orEmpty()
        }.take(80)
        val venue = labelled(venueLabels).take(100)
        val address = labelled(addressLabels).take(160)
        if (venue.isBlank()) warnings += "Venue was not identified. Enter it from the poster."
        if (address.isBlank()) warnings += "Address was not identified. Confirm the locality."
        val dates = linkedSetOf<LocalDate>()
        fun addDate(year: String, month: String, day: String) {
            runCatching { LocalDate.of(year.toInt(), month.toInt(), day.toInt()) }.getOrNull()?.let(dates::add)
        }
        Regex("(?<!\\d)(20\\d{2})[-/](\\d{1,2})[-/](\\d{1,2})(?!\\d)").findAll(text).forEach {
            addDate(it.groupValues[1], it.groupValues[2], it.groupValues[3])
        }
        Regex("(?<!\\d)(\\d{1,2})[./-](\\d{1,2})[./-](20\\d{2})(?!\\d)").findAll(text).forEach {
            addDate(it.groupValues[3], it.groupValues[2], it.groupValues[1])
        }
        Regex("(?<!\\d)(\\d{1,2})(?:st|nd|rd|th)?\\s*($monthPattern)[,\\s]+(20\\d{2})(?!\\d)", RegexOption.IGNORE_CASE)
            .findAll(text).forEach { match ->
                val month = monthNames.indexOfFirst { Regex("^(?:$it)$", RegexOption.IGNORE_CASE).matches(match.groupValues[2]) } + 1
                addDate(match.groupValues[3], month.toString(), match.groupValues[1])
            }
        Regex("($monthPattern)\\s+(\\d{1,2})(?:st|nd|rd|th)?[,\\s]+(20\\d{2})(?!\\d)", RegexOption.IGNORE_CASE)
            .findAll(text).forEach { match ->
                val month = monthNames.indexOfFirst { Regex("^(?:$it)$", RegexOption.IGNORE_CASE).matches(match.groupValues[1]) } + 1
                addDate(match.groupValues[3], month.toString(), match.groupValues[2])
            }
        val isRange = Regex("\\d{1,2}\\s*(?:-|–|to|ರಿಂದ|से)\\s*\\d{1,2}\\s*(?:$monthPattern)", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val date = dates.singleOrNull().takeUnless { isRange }
        if (date == null) warnings += if (dates.size > 1 || isRange) "Multiple dates or a date range found. Select the event start and end." else "A full date with year was not found. Choose the date; no year has been assumed."

        val times = linkedSetOf<LocalTime>()
        Regex("(?<!\\d)(\\d{1,2})(?:[:.](\\d{2}))?\\s*(a\\.?m\\.?|p\\.?m\\.?)(?![a-z])", RegexOption.IGNORE_CASE)
            .findAll(text).forEach {
                val hour = it.groupValues[1].toInt()
                val minute = it.groupValues[2].ifBlank { "0" }.toInt()
                if (hour in 1..12 && minute in 0..59) times += LocalTime.of(hour % 12 + if (it.groupValues[3].lowercase(Locale.ROOT).startsWith("p")) 12 else 0, minute)
            }
        Regex("(?<!\\d)([01]?\\d|2[0-3]):([0-5]\\d)(?!\\d)\\s*(?![ap]\\.?m)", RegexOption.IGNORE_CASE).findAll(text).forEach {
            val hour = it.groupValues[1].toInt()
            // A bare 1–12 hour could mean morning or evening. Do not silently choose.
            if (hour == 0 || hour > 12) times += LocalTime.of(hour, it.groupValues[2].toInt())
        }
        Regex("(ಬೆಳಿಗ್ಗೆ|ಮುಂಜಾನೆ|ಸಂಜೆ|ರಾತ್ರಿ|सुबह|शाम|रात)\\s*(\\d{1,2})(?:[:.](\\d{2}))?").findAll(text).forEach {
            val hour = it.groupValues[2].toInt(); val minute = it.groupValues[3].ifBlank { "0" }.toInt()
            if (hour in 1..12 && minute in 0..59) times += LocalTime.of(hour % 12 + if (it.groupValues[1] in listOf("ಸಂಜೆ", "ರಾತ್ರಿ", "शाम", "रात")) 12 else 0, minute)
        }
        val time = times.singleOrNull()
        if (time == null) warnings += if (times.size > 1) "Several times found. Confirm which is the event start time." else "Start time is missing or ambiguous. Choose it explicitly."
        warnings += "Confirm the end date and time; no duration has been assumed."
        val category = when {
            containsAny(text, "concert", "music", "ಸಂಗೀತ", "ಸಂಗೀತೋತ್ಸವ", "संगीत") -> EventCategory.CONCERT
            containsAny(text, "workshop", "training", "ಕಾರ್ಯಾಗಾರ", "कार्यशाला") -> EventCategory.WORKSHOP
            containsAny(text, "sale", "discount", "ರಿಯಾಯಿತಿ", "ಬಟ್ಟೆ", "छूट") -> EventCategory.SALE
            containsAny(text, "sports", "cricket", "football", "ಕ್ರೀಡಾ", "ಕ್ರಿಕೆಟ್", "खेल") -> EventCategory.SPORTS
            containsAny(text, "festival", "ಜಾತ್ರೆ", "ಉತ್ಸವ", "ರಥ", "महोत्सव", "उत्सव") -> EventCategory.FESTIVAL
            else -> EventCategory.COMMUNITY
        }
        return PosterDraft(text, title, lines.joinToString(" · ").take(300), venue, address, category,
            date?.toString(), time?.toString(), null, null, warnings)
    }

    private fun containsAny(text: String, vararg words: String) = words.any { text.contains(it, ignoreCase = true) }
}
