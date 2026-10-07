package com.poi.core.poster

import com.poi.core.model.EventCategory
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class PosterLanguage(val label: String, val models: String) {
    AUTO("Mixed / automatic", "kan+eng+hin"),
    KANNADA("Kannada + English", "kan+eng"),
    HINDI("Hindi + English", "hin+eng"),
    ENGLISH("English", "eng"),
}

data class PosterDraft(
    val sourceText: String,
    val title: String,
    val summary: String,
    val venue: String,
    val address: String,
    val category: EventCategory,
    val date: String?,
    val time: String?,
    val endDate: String?,
    val endTime: String?,
    val warnings: List<String>,
    val dateSuggestions: List<PosterDateSuggestion> = emptyList(),
    val timeSuggestions: List<String> = emptyList(),
    val continuousDateRange: Boolean = false,
)

data class PosterDateSuggestion(val month: Int, val day: Int, val year: Int?) {
    fun resolve(confirmedYear: Int? = year): String? = runCatching {
        LocalDate.of(confirmedYear ?: error("Confirm the year"), month, day).toString()
    }.getOrNull()
    val label: String get() = "%02d/%02d%s".format(day, month, year?.let { "/$it" }.orEmpty())
}

data class PosterScan(val draft: PosterDraft, val weakRecognition: Boolean)

fun eventTimestamp(date: String, time: String, zone: ZoneId = ZoneId.systemDefault()): Long? =
    runCatching { LocalDate.parse(date.trim()).atTime(LocalTime.parse(time.trim())).atZone(zone).toInstant().toEpochMilli() }.getOrNull()
