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
            lines.filter {
                it.length in 4..100 && it.count(Char::isLetter) >= 4 &&
                    !Regex("^(?:$venueLabels|$addressLabels|date|time|contact|phone|ದಿನಾಂಕ|ಸಮಯ|संपर्क|दिनांक|समय)", RegexOption.IGNORE_CASE).containsMatchIn(it) &&
                    !Regex("\\d{4}|https?://|www\\.").containsMatchIn(it) &&
                    !containsAny(it, "what's happening", "this weekend", "public demand", "book your", "tickets", "daily", "only for", "qr code", "location", "presents", "designs", "ಸನ್ನಿಧಿ", "ಪ್ರಸನ್ನ")
            }.maxByOrNull { line ->
                (if (containsAny(line, "kambala", "concert", "fan park", "circus", "festival", "workshop", "ಕಂಬಳ", "ಸರ್ಕಸ್", "ನೇಮ", "ಜಾತ್ರೆ", "उत्सव")) 100 else 0) +
                    (if (line.count(Char::isUpperCase) > line.count(Char::isLowerCase)) 15 else 0) -
                    (if (line.contains(',')) 20 else 0)
            }.orEmpty()
        }.take(80)
        val placeLines = lines.filter { line ->
            line != title && line.count(Char::isLetter) >= 4 &&
                !containsAny(line, "public demand", "book", "tickets", "happening", "weekend", "presents", "concert", "daily", "copyright", "designs") &&
                (containsAny(line, "ground", "beach", "guthu", "convention", "auditorium", "hall", "road", "temple", "ಮೈದಾನ", "ಬೀಚ್", "ದೇವಸ್ಥಾನ", "ಸ್ಥಳ", "सभागार", "मैदान") ||
                    (line.contains(',') && !Regex("\\d").containsMatchIn(line)))
        }.takeLast(2)
        val venue = labelled(venueLabels).ifBlank { placeLines.firstOrNull().orEmpty().replace("📍", "").trim() }.take(100)
        val address = labelled(addressLabels).ifBlank { placeLines.joinToString(", ") }.take(160)
        if (venue.isNotBlank() && labelled(venueLabels).isBlank()) warnings += "Location is a suggestion from unlabelled poster text. Confirm the venue/locality."
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
        val suggestions = extractDateSuggestions(text)
        val date = if (suggestions.size == 1) suggestions.single().resolve()?.let(LocalDate::parse) else dates.singleOrNull().takeUnless { isRange || suggestions.size > 1 }
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
        if (suggestions.any { it.year == null }) warnings += "The poster omits its year. Confirm a year to use the detected days; it will not be assumed."
        val continuousRange = Regex("(?:$monthPattern)\\s*[:.]?\\s*\\d{1,2}\\s*[-–]\\s*\\d{1,2}(?!\\d)|\\d{1,2}\\s*[-–]\\s*\\d{1,2}\\s*(?:$monthPattern)|ರಿಂದ|ವರೆಗೆ|\\bto\\b", RegexOption.IGNORE_CASE)
            .containsMatchIn(text) && suggestions.size > 1
        if (suggestions.size > 1) warnings += if (continuousRange) "Date range detected. Confirm its first and last day." else "Separate advertised dates detected. Choose one occurrence; do not treat the intervening days as events."
        val category = when {
            containsAny(text, "concert", "music", "ಸಂಗೀತ", "ಸಂಗೀತೋತ್ಸವ", "संगीत") -> EventCategory.CONCERT
            containsAny(text, "workshop", "training", "ಕಾರ್ಯಾಗಾರ", "कार्यशाला") -> EventCategory.WORKSHOP
            containsAny(text, "sale", "discount", "ರಿಯಾಯಿತಿ", "ಬಟ್ಟೆ", "छूट") -> EventCategory.SALE
            containsAny(text, "sports", "cricket", "football", "kambala", "fan park", "ಕಂಬಳ", "ಕ್ರೀಡಾ", "ಕ್ರಿಕೆಟ್", "खेल") -> EventCategory.SPORTS
            containsAny(text, "festival", "ಜಾತ್ರೆ", "ಉತ್ಸವ", "ರಥ", "महोत्सव", "उत्सव") -> EventCategory.FESTIVAL
            else -> EventCategory.COMMUNITY
        }
        return PosterDraft(text, title, lines.joinToString(" · ").take(300), venue, address, category,
            date?.toString(), time?.toString(), null, null, warnings, suggestions, times.map(LocalTime::toString), continuousRange)
    }

    private fun extractDateSuggestions(text: String): List<PosterDateSuggestion> {
        val found = linkedSetOf<PosterDateSuggestion>()
        val years = Regex("(?<!\\d)20\\d{2}(?!\\d)").findAll(text).map { it.value.toInt() }.toSet()
        val sharedYear = years.singleOrNull()
        fun add(month: Int, day: Int, year: Int? = sharedYear) {
            if (runCatching { LocalDate.of(year ?: 2000, month, day) }.isSuccess) found += PosterDateSuggestion(month, day, year)
        }
        Regex("(?<!\\d)(20\\d{2})[-/](\\d{1,2})[-/](\\d{1,2})(?!\\d)").findAll(text).forEach {
            add(it.groupValues[2].toInt(), it.groupValues[3].toInt(), it.groupValues[1].toInt())
        }
        Regex("(?<!\\d)(\\d{1,2})[./-](\\d{1,2})[./-](20\\d{2})(?!\\d)").findAll(text).forEach {
            add(it.groupValues[2].toInt(), it.groupValues[1].toInt(), it.groupValues[3].toInt())
        }
        fun month(value: String) = monthNames.indexOfFirst { Regex("^(?:$it)$", RegexOption.IGNORE_CASE).matches(value) } + 1
        // Month-first lists: Apr 04 & 05; May 29 & 31; AUGUST: 22,23,... SEPT:4,5,6.
        Regex("(?<![a-z])($monthPattern)\\s*[:.]*\\s*((?:\\d{1,2}(?:st|nd|rd|th)?)(?:\\s*(?:,|&|[-–]|and|to)\\s*\\d{1,2}(?:st|nd|rd|th)?)*)(?!\\d)", RegexOption.IGNORE_CASE)
            .findAll(text).forEach { match ->
                val days = Regex("\\d{1,2}").findAll(match.groupValues[2]).map { it.value.toInt() }.toList()
                days.forEach { add(month(match.groupValues[1]), it) }
            }
        // Day-first lists: 07-08 March; 19 April. A year on another line is retained for review.
        Regex("(?<!\\d)(\\d{1,2})(?:st|nd|rd|th)?(?:\\s*([-–&]|to)\\s*(\\d{1,2}))?\\s+($monthPattern)(?![a-z])", RegexOption.IGNORE_CASE)
            .findAll(text).forEach { match ->
                add(month(match.groupValues[4]), match.groupValues[1].toInt())
                match.groupValues[3].toIntOrNull()?.let { add(month(match.groupValues[4]), it) }
            }
        return found.toList()
    }

    private fun containsAny(text: String, vararg words: String) = words.any { text.contains(it, ignoreCase = true) }
}
