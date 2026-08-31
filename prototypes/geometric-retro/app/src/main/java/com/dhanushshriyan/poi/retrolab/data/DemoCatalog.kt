package com.dhanushshriyan.poi.retrolab.data

import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

enum class Category(val label: String) {
    DESIGN("Art & design"), MUSIC("Music"), MARKET("Markets"),
}
enum class EventImage { ARCHITECTURE, MUSIC, MARKET }
enum class Rsvp(val label: String) {
    NONE("No plan"), INTERESTED("Interested"), GOING("Going"), HERE("I'm here"),
}
data class DemoEvent(
    val id: String,
    val title: String,
    val category: Category,
    val date: LocalDate,
    val time: LocalTime,
    val venue: String,
    val distanceKm: Double,
    val host: String,
    val description: String,
    val note: String,
    val image: EventImage,
    val going: Int,
    val admission: String,
)
data class DemoMoment(
    val id: String,
    val eventId: String,
    val author: String,
    val initials: String,
    val caption: String,
    val image: EventImage,
    val likes: Int,
    val comments: List<String>,
)
fun demoCatalog(today: LocalDate): List<DemoEvent> = listOf(
    DemoEvent(
        "form-house", "Form / Function", Category.DESIGN, today, LocalTime.of(16, 0),
        "The Form House", 2.4, "THE FORM COLLECTIVE",
        "Step inside a modernist coastal pavilion. A slow architectural walk, a conversation with local designers, and beautiful spaces to linger in.",
        "Open-house tour · 90 minutes · bring your curiosity",
        EventImage.ARCHITECTURE, 86, "FREE ENTRY",
    ),
    DemoEvent(
        "analog-hours", "Analog After Hours", Category.MUSIC, today, LocalTime.of(19, 30),
        "Courtyard No. 6", 4.8, "SIDE A SESSIONS",
        "Vinyl selections meet a live saxophone in a sun-warmed courtyard. Discover a new sound, find your people, stay for one more record.",
        "Courtyard session · standing & seated · adults 18+",
        EventImage.MUSIC, 124, "DEMO EVENT",
    ),
    DemoEvent(
        "made-local", "Made Local Market", Category.MARKET, today.plusDays(1), LocalTime.of(11, 0),
        "Foundry Hall", 6.2, "THE LOCAL EDIT",
        "Ceramics, bold prints and small-batch objects from independent makers. Meet the hands behind the work, with coffee and conversation on the side.",
        "Makers market · indoor venue · all-day browsing",
        EventImage.MARKET, 213, "FREE ENTRY",
    ),
    DemoEvent(
        "clay-club", "The Clay Club", Category.MARKET, today.plusDays(2), LocalTime.of(10, 30),
        "Studio Quarter", 9.1, "THE LOCAL EDIT",
        "A hands-on morning of simple forms and imperfect pots. This sample workshop gives the theme another kind of event to show off.",
        "Workshop · beginner friendly · sample listing only",
        EventImage.MARKET, 32, "DEMO EVENT",
    ),
    DemoEvent(
        "open-studio", "Open Studio Sunday", Category.DESIGN, today.plusDays(3), LocalTime.of(14, 0),
        "Coastal Design House", 12.5, "THE FORM COLLECTIVE",
        "Sketchbooks, models and ideas in progress. Wander through an open studio and see how a building starts with a conversation.",
        "Open studio · drop in · sample listing only",
        EventImage.ARCHITECTURE, 65, "FREE ENTRY",
    ),
)
val demoMoments = listOf(
    DemoMoment(
        "moment-form", "form-house", "@THE_GRID_ISSUE", "GI",
        "Good light. Better company. A different way to spend the afternoon. #FormAndFunction",
        EventImage.ARCHITECTURE, 248, listOf("Maya: That circular window!", "Arjun: Putting this on my list."),
    ),
    DemoMoment(
        "moment-music", "analog-hours", "@SIDE_A_STORIES", "SA",
        "One more record turned into three. This courtyard has a rhythm of its own.",
        EventImage.MUSIC, 186, listOf("Neha: This is my kind of evening."),
    ),
    DemoMoment(
        "moment-market", "made-local", "@MADE_BY_US", "MU",
        "Came for a coffee. Left with a new favourite cup and a story from its maker.",
        EventImage.MARKET, 312, listOf("Dev: Love these colours."),
    ),
)
fun filterEvents(
    events: List<DemoEvent>,
    query: String = "",
    category: Category? = null,
    maxDistanceKm: Int? = null,
    date: LocalDate? = null,
): List<DemoEvent> {
    val search = query.trim().lowercase(Locale.ROOT)
    return events.filter { event ->
        (category == null || event.category == category) &&
            (maxDistanceKm == null || event.distanceKm <= maxDistanceKm) &&
            (date == null || event.date == date) &&
            (search.isEmpty() || listOf(event.title, event.venue, event.host, event.category.label)
                .any { it.lowercase(Locale.ROOT).contains(search) })
    }.sortedWith(compareBy(DemoEvent::date, DemoEvent::time))
}
fun toggleRsvp(current: Rsvp, selected: Rsvp): Rsvp =
    if (current == selected) Rsvp.NONE else selected

fun normalizeComment(value: String): String? =
    value.trim().takeIf { it.isNotEmpty() && it.length <= 280 }

