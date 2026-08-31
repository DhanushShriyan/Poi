package com.dhanushshriyan.poi.scribble

import androidx.annotation.DrawableRes

enum class CrowdIntent(val label: String) {
    NONE("No plan"),
    INTERESTED("Interested"),
    GOING("Going"),
    HERE("I'm here"),
}

data class DemoEvent(
    val id: String,
    val day: Int,
    val month: String,
    val time: String,
    val title: String,
    val place: String,
    val distanceKm: Int,
    val category: String,
    val organizer: String,
    val description: String,
    val interestedCount: Int,
    val goingCount: Int,
    @DrawableRes val imageRes: Int,
    val cardTilt: Float,
)

data class DatePage(
    val day: Int,
    val month: String,
    val note: String,
)

val datePages = listOf(
    DatePage(26, "AUG", "TODAY"),
    DatePage(27, "AUG", "2 PICKS"),
    DatePage(28, "AUG", "FRIDAY"),
    DatePage(29, "AUG", "WEEKEND"),
)

val demoEvents = listOf(
    DemoEvent(
        id = "rooftop-radio",
        day = 26,
        month = "AUG",
        time = "7:30 PM",
        title = "Rooftop Radio",
        place = "Lighthouse Terrace",
        distanceKm = 3,
        category = "LIVE MUSIC",
        organizer = "Coastline Sessions",
        description = "A tiny sunset stage, a new indie set and room for the kind of conversations that run late.",
        interestedCount = 284,
        goingCount = 96,
        imageRes = R.drawable.event_rooftop,
        cardTilt = -0.45f,
    ),
    DemoEvent(
        id = "makers-market",
        day = 26,
        month = "AUG",
        time = "11:00 AM",
        title = "Sunday-ish Makers Market",
        place = "Riverfront Warehouse",
        distanceKm = 6,
        category = "MARKET",
        organizer = "Made Around Here",
        description = "Fresh bakes, ceramics, small-run art and twenty local makers under one very colourful roof.",
        interestedCount = 418,
        goingCount = 173,
        imageRes = R.drawable.event_market,
        cardTilt = 0.35f,
    ),
    DemoEvent(
        id = "sunrise-shoreline",
        day = 27,
        month = "AUG",
        time = "6:20 AM",
        title = "Sunrise & Shoreline",
        place = "Someshwara Beach",
        distanceKm = 14,
        category = "COMMUNITY",
        organizer = "Good Tides Club",
        description = "A gentle beach clean-up followed by fruit, coffee and a sunrise picnic. Gloves and good music provided.",
        interestedCount = 191,
        goingCount = 72,
        imageRes = R.drawable.event_beach,
        cardTilt = -0.25f,
    ),
)

fun filterDemoEvents(
    events: List<DemoEvent>,
    selectedDay: Int,
    query: String,
    maxDistanceKm: Int?,
): List<DemoEvent> {
    val normalizedQuery = query.trim().lowercase()
    return events.filter { event ->
        event.day == selectedDay &&
            (maxDistanceKm == null || event.distanceKm <= maxDistanceKm) &&
            (normalizedQuery.isBlank() || listOf(
                event.title,
                event.place,
                event.category,
                event.organizer,
            ).any { it.lowercase().contains(normalizedQuery) })
    }
}

fun nextRadius(current: Int?, choices: List<Int?> = listOf(5, 15, 25, null)): Int? {
    val index = choices.indexOf(current).takeIf { it >= 0 } ?: 0
    return choices[(index + 1) % choices.size]
}
