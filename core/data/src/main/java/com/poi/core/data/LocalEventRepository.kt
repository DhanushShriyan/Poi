package com.poi.core.data

import android.content.Context
import com.poi.core.model.AppSettings
import com.poi.core.model.AttendanceStatus
import com.poi.core.model.CheckInVisibility
import com.poi.core.model.Event
import com.poi.core.model.EventCategory
import com.poi.core.model.EventReport
import com.poi.core.model.EventVisibility
import com.poi.core.model.NewEvent
import com.poi.core.model.Organizer
import com.poi.core.model.UserProfile
import com.poi.core.model.VerificationLevel
import com.poi.core.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

class LocalEventRepository(context: Context) : EventRepository {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val seedEvents = createSeedEvents()
    private val userEvents = mutableListOf<Event>()
    private val eventOverrides = mutableMapOf<String, Event>()
    private val deletedIds = preferences.getStringSet(KEY_DELETED_EVENTS, emptySet()).orEmpty().toMutableSet()
    private val reportedIds = preferences.getStringSet(KEY_REPORTS, emptySet()).orEmpty().toMutableSet()

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    override val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _allEvents = MutableStateFlow<List<Event>>(emptyList())
    override val allEvents: StateFlow<List<Event>> = _allEvents.asStateFlow()

    private val _reportedEvents = MutableStateFlow<List<EventReport>>(emptyList())
    override val reportedEvents: StateFlow<List<EventReport>> = _reportedEvents.asStateFlow()

    private val _attendance = MutableStateFlow(loadAttendance())
    override val attendance: StateFlow<Map<String, AttendanceStatus>> = _attendance.asStateFlow()

    private val _checkInVisibility = MutableStateFlow(loadCheckInVisibility())
    override val checkInVisibility: StateFlow<Map<String, CheckInVisibility>> =
        _checkInVisibility.asStateFlow()

    private val _settings = MutableStateFlow(loadSettings())
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _profile = MutableStateFlow(
        UserProfile(
            id = "local-user",
            displayName = "Dhanush",
            handle = "@dhanush",
            homeArea = "Mangaluru",
            attendedCount = 12,
            hostedCount = 3,
            contributionPoints = 240,
        ),
    )
    override val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    init {
        userEvents += loadUserEvents()
        eventOverrides += loadEventOverrides().associateBy(Event::id)
        refreshEvents()
        refreshProfile()
    }

    override suspend fun setAttendance(
        eventId: String,
        status: AttendanceStatus,
        visibility: CheckInVisibility?,
    ) {
        _attendance.value = _attendance.value.toMutableMap().apply {
            if (status == AttendanceStatus.NONE) remove(eventId) else put(eventId, status)
        }
        if (visibility != null) {
            _checkInVisibility.value = _checkInVisibility.value.toMutableMap().apply {
                put(eventId, visibility)
            }
        }
        persistAttendance()
        refreshProfile()
    }

    override suspend fun createEvent(newEvent: NewEvent): Event {
        val event = Event(
            id = "local-${UUID.randomUUID()}",
            title = newEvent.title.trim(),
            summary = newEvent.summary.trim(),
            description = newEvent.summary.trim(),
            category = newEvent.category,
            startsAtMillis = newEvent.startsAtMillis,
            endsAtMillis = newEvent.endsAtMillis,
            venue = newEvent.venue.trim(),
            address = newEvent.address.trim(),
            distanceKm = 0.0,
            organizer = Organizer(newEvent.organizerName, false),
            visibility = newEvent.visibility,
            verification = VerificationLevel.COMMUNITY,
            attendeeCount = 1,
            friendNames = emptyList(),
            themeKey = themeFor(newEvent.category),
            createdByCurrentUser = true,
        )
        userEvents += event
        persistUserEvents()
        refreshEvents()
        refreshProfile()
        return event
    }

    override suspend fun reportEvent(eventId: String, reason: String) {
        reportedIds += eventId
        preferences.edit()
            .putStringSet(KEY_REPORTS, reportedIds)
            .putString("report_reason_$eventId", reason)
            .putLong("report_time_$eventId", System.currentTimeMillis())
            .apply()
        refreshEvents()
    }

    override suspend fun restoreReportedEvent(eventId: String) {
        reportedIds -= eventId
        preferences.edit()
            .putStringSet(KEY_REPORTS, reportedIds)
            .remove("report_reason_$eventId")
            .remove("report_time_$eventId")
            .apply()
        refreshEvents()
    }

    override suspend fun updateEvent(event: Event) {
        val updated = event.copy(updatedAtMillis = System.currentTimeMillis())
        val userEventIndex = userEvents.indexOfFirst { it.id == event.id }
        if (userEventIndex >= 0) {
            userEvents[userEventIndex] = updated
            persistUserEvents()
        } else {
            require(seedEvents.any { it.id == event.id }) { "Unknown event ${event.id}" }
            eventOverrides[event.id] = updated
            persistEventOverrides()
        }
        refreshEvents()
    }

    override suspend fun deleteEvent(eventId: String) {
        val removedUserEvent = userEvents.removeAll { it.id == eventId }
        if (removedUserEvent) {
            persistUserEvents()
        } else {
            deletedIds += eventId
            preferences.edit().putStringSet(KEY_DELETED_EVENTS, deletedIds).apply()
        }
        eventOverrides.remove(eventId)
        persistEventOverrides()
        reportedIds.remove(eventId)
        _attendance.value = _attendance.value - eventId
        _checkInVisibility.value = _checkInVisibility.value - eventId
        persistAttendance()
        refreshEvents()
        refreshProfile()
    }

    override suspend fun updateSettings(settings: AppSettings) {
        _settings.value = settings
        preferences.edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putInt(KEY_DISCOVERY_RADIUS_KM, settings.discoveryRadiusKm ?: ANY_DISTANCE)
            .putString(KEY_DEFAULT_VISIBILITY, settings.defaultCheckInVisibility.name)
            .putBoolean(KEY_SHOW_PLANS, settings.showPlansToFriends)
            .putBoolean(KEY_REMINDERS, settings.eventReminders)
            .putBoolean(KEY_DIGEST, settings.weeklyDigest)
            .putBoolean(KEY_FRIEND_ACTIVITY, settings.friendActivity)
            .apply()
    }

    private fun refreshEvents() {
        val merged = (seedEvents + userEvents)
            .map { event -> eventOverrides[event.id] ?: event }
            .filterNot { it.id in deletedIds }
            .sortedBy { it.startsAtMillis }
        _allEvents.value = merged
        _events.value = merged.filterNot { it.id in reportedIds }
        _reportedEvents.value = reportedIds.mapNotNull { eventId ->
            merged.firstOrNull { it.id == eventId }?.let {
                EventReport(
                    eventId = eventId,
                    reason = preferences.getString("report_reason_$eventId", "Not specified").orEmpty(),
                    reportedAtMillis = preferences.getLong("report_time_$eventId", 0L),
                )
            }
        }.sortedByDescending { it.reportedAtMillis }
    }

    private fun refreshProfile() {
        val attended = _attendance.value.values.count {
            it == AttendanceStatus.ATTENDED || it == AttendanceStatus.HERE
        }
        _profile.value = _profile.value.copy(
            attendedCount = 12 + attended,
            hostedCount = 3 + userEvents.size,
        )
    }

    private fun loadAttendance(): Map<String, AttendanceStatus> =
        preferences.getStringSet(KEY_ATTENDANCE, emptySet()).orEmpty().mapNotNull { encoded ->
            val parts = encoded.split('|')
            if (parts.size != 2) return@mapNotNull null
            val status = runCatching { AttendanceStatus.valueOf(parts[1]) }.getOrNull()
            status?.let { parts[0] to it }
        }.toMap()

    private fun loadCheckInVisibility(): Map<String, CheckInVisibility> =
        preferences.getStringSet(KEY_CHECK_IN_VISIBILITY, emptySet()).orEmpty().mapNotNull { encoded ->
            val parts = encoded.split('|')
            if (parts.size != 2) return@mapNotNull null
            val visibility = runCatching { CheckInVisibility.valueOf(parts[1]) }.getOrNull()
            visibility?.let { parts[0] to it }
        }.toMap()

    private fun persistAttendance() {
        preferences.edit()
            .putStringSet(KEY_ATTENDANCE, _attendance.value.map { "${it.key}|${it.value.name}" }.toSet())
            .putStringSet(
                KEY_CHECK_IN_VISIBILITY,
                _checkInVisibility.value.map { "${it.key}|${it.value.name}" }.toSet(),
            )
            .apply()
    }

    private fun loadSettings(): AppSettings = AppSettings(
        themeMode = runCatching {
            ThemeMode.valueOf(preferences.getString(KEY_THEME_MODE, ThemeMode.LIGHT.name).orEmpty())
        }.getOrDefault(ThemeMode.LIGHT),
        discoveryRadiusKm = preferences.getInt(KEY_DISCOVERY_RADIUS_KM, 25)
            .takeUnless { it == ANY_DISTANCE },
        defaultCheckInVisibility = runCatching {
            CheckInVisibility.valueOf(
                preferences.getString(KEY_DEFAULT_VISIBILITY, CheckInVisibility.FRIENDS.name).orEmpty(),
            )
        }.getOrDefault(CheckInVisibility.FRIENDS),
        showPlansToFriends = preferences.getBoolean(KEY_SHOW_PLANS, true),
        eventReminders = preferences.getBoolean(KEY_REMINDERS, true),
        weeklyDigest = preferences.getBoolean(KEY_DIGEST, true),
        friendActivity = preferences.getBoolean(KEY_FRIEND_ACTIVITY, true),
    )

    private fun persistUserEvents() {
        val array = JSONArray()
        userEvents.forEach { event -> array.put(event.toJson()) }
        preferences.edit().putString(KEY_USER_EVENTS, array.toString()).apply()
    }

    private fun loadUserEvents(): List<Event> {
        val encoded = preferences.getString(KEY_USER_EVENTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            buildList {
                repeat(array.length()) { index ->
                    add(array.getJSONObject(index).toEvent(createdByCurrentUserFallback = true))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun persistEventOverrides() {
        val array = JSONArray()
        eventOverrides.values.forEach { event -> array.put(event.toJson()) }
        preferences.edit().putString(KEY_EVENT_OVERRIDES, array.toString()).apply()
    }

    private fun loadEventOverrides(): List<Event> {
        val encoded = preferences.getString(KEY_EVENT_OVERRIDES, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(encoded)
            buildList {
                repeat(array.length()) { index -> add(array.getJSONObject(index).toEvent()) }
            }
        }.getOrDefault(emptyList())
    }

    private fun Event.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("summary", summary)
        put("description", description)
        put("category", category.name)
        put("starts", startsAtMillis)
        put("ends", endsAtMillis)
        put("venue", venue)
        put("address", address)
        put("distance", distanceKm)
        put("organizer", organizer.name)
        put("organizerVerified", organizer.isVerified)
        put("visibility", visibility.name)
        put("verification", verification.name)
        put("attendees", attendeeCount)
        put("friends", JSONArray(friendNames))
        put("theme", themeKey)
        put("featured", featured)
        put("createdByCurrentUser", createdByCurrentUser)
        put("cancelled", isCancelled)
        updatedAtMillis?.let { put("updated", it) }
    }

    private fun JSONObject.toEvent(createdByCurrentUserFallback: Boolean = false): Event {
        val category = EventCategory.valueOf(getString("category"))
        val friends = optJSONArray("friends")
        return Event(
            id = getString("id"),
            title = getString("title"),
            summary = getString("summary"),
            description = optString("description", getString("summary")),
            category = category,
            startsAtMillis = getLong("starts"),
            endsAtMillis = getLong("ends"),
            venue = getString("venue"),
            address = getString("address"),
            distanceKm = optDouble("distance", 0.0),
            organizer = Organizer(
                name = optString("organizer", "Community organizer"),
                isVerified = optBoolean("organizerVerified", false),
            ),
            visibility = EventVisibility.valueOf(getString("visibility")),
            verification = runCatching {
                VerificationLevel.valueOf(optString("verification", VerificationLevel.COMMUNITY.name))
            }.getOrDefault(VerificationLevel.COMMUNITY),
            attendeeCount = optInt("attendees", 1),
            friendNames = buildList {
                if (friends != null) repeat(friends.length()) { index -> add(friends.getString(index)) }
            },
            themeKey = optString("theme", themeFor(category)),
            featured = optBoolean("featured", false),
            createdByCurrentUser = optBoolean("createdByCurrentUser", createdByCurrentUserFallback),
            isCancelled = optBoolean("cancelled", false),
            updatedAtMillis = if (has("updated")) optLong("updated") else null,
        )
    }

    companion object {
        private const val PREFS_NAME = "poi_local_store"
        private const val KEY_ATTENDANCE = "attendance"
        private const val KEY_CHECK_IN_VISIBILITY = "check_in_visibility"
        private const val KEY_USER_EVENTS = "user_events"
        private const val KEY_EVENT_OVERRIDES = "event_overrides"
        private const val KEY_DELETED_EVENTS = "deleted_events"
        private const val KEY_REPORTS = "reported_events"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_DISCOVERY_RADIUS_KM = "discovery_radius_km"
        private const val KEY_DEFAULT_VISIBILITY = "default_visibility"
        private const val KEY_SHOW_PLANS = "show_plans"
        private const val KEY_REMINDERS = "reminders"
        private const val KEY_DIGEST = "weekly_digest"
        private const val KEY_FRIEND_ACTIVITY = "friend_activity"
        private const val ANY_DISTANCE = -1
    }
}

private fun createSeedEvents(): List<Event> {
    val now = System.currentTimeMillis()
    val hour = TimeUnit.HOURS.toMillis(1)
    val day = TimeUnit.DAYS.toMillis(1)
    return listOf(
        Event(
            id = "kudla-festival",
            title = "Kudla Village Festival",
            summary = "Food stalls, folk performances and a community procession.",
            description = "A three-day celebration of coastal culture with local food, crafts, music and evening performances. Entry is free for everyone.",
            category = EventCategory.FESTIVAL,
            startsAtMillis = now - hour,
            endsAtMillis = now + 7 * hour,
            venue = "Kadri Grounds",
            address = "Kadri, Mangaluru",
            distanceKm = 2.4,
            organizer = Organizer("Kadri Community Council", true),
            visibility = EventVisibility.PUBLIC,
            verification = VerificationLevel.OFFICIAL,
            attendeeCount = 428,
            friendNames = listOf("Ananya", "Rohan", "Meera"),
            themeKey = "festival",
            featured = true,
        ),
        Event(
            id = "coastal-concert",
            title = "Coastal Music Under the Stars",
            summary = "An open-air evening with indie and folk artists.",
            description = "Bring a mat and enjoy performances from independent musicians across coastal Karnataka. Gates open at 5:30 PM.",
            category = EventCategory.CONCERT,
            startsAtMillis = now + day,
            endsAtMillis = now + day + 5 * hour,
            venue = "Tannirbhavi Beach",
            address = "Tannirbhavi, Mangaluru",
            distanceKm = 7.8,
            organizer = Organizer("Coastline Collective", true),
            visibility = EventVisibility.PUBLIC,
            verification = VerificationLevel.ORGANIZER,
            attendeeCount = 216,
            friendNames = listOf("Arjun", "Nisha"),
            themeKey = "concert",
        ),
        Event(
            id = "monsoon-sale",
            title = "Monsoon Street Sale",
            summary = "Local shops, handmade products and end-of-season offers.",
            description = "A neighbourhood shopping street featuring verified local sellers, handmade products, food counters and family activities.",
            category = EventCategory.SALE,
            startsAtMillis = now + 2 * day,
            endsAtMillis = now + 2 * day + 9 * hour,
            venue = "Car Street",
            address = "Hampankatta, Mangaluru",
            distanceKm = 1.3,
            organizer = Organizer("Local Traders Association", true),
            visibility = EventVisibility.PUBLIC,
            verification = VerificationLevel.CONFIRMED,
            attendeeCount = 94,
            friendNames = listOf("Meera"),
            themeKey = "sale",
        ),
        Event(
            id = "beach-cleanup",
            title = "Sunday Beach Cleanup",
            summary = "A community cleanup followed by breakfast.",
            description = "Gloves and collection bags are provided. Wear comfortable footwear and bring your own water bottle.",
            category = EventCategory.COMMUNITY,
            startsAtMillis = now + 3 * day,
            endsAtMillis = now + 3 * day + 3 * hour,
            venue = "Panambur Beach",
            address = "Panambur, Mangaluru",
            distanceKm = 9.1,
            organizer = Organizer("Clean Coast Mangaluru", true),
            visibility = EventVisibility.PUBLIC,
            verification = VerificationLevel.ORGANIZER,
            attendeeCount = 67,
            friendNames = listOf("Rohan", "Fatima"),
            themeKey = "community",
        ),
        Event(
            id = "football-cup",
            title = "District Five-a-side Cup",
            summary = "Local teams compete in a one-day football tournament.",
            description = "Group-stage matches begin at 8 AM. Spectator entry is free. Refreshments are available at the ground.",
            category = EventCategory.SPORTS,
            startsAtMillis = now + 4 * day,
            endsAtMillis = now + 4 * day + 10 * hour,
            venue = "Nehru Maidan",
            address = "State Bank, Mangaluru",
            distanceKm = 2.0,
            organizer = Organizer("District Sports Club", false),
            visibility = EventVisibility.PUBLIC,
            verification = VerificationLevel.COMMUNITY,
            attendeeCount = 143,
            friendNames = emptyList(),
            themeKey = "sports",
        ),
        Event(
            id = "pottery-workshop",
            title = "Clay & Chai Workshop",
            summary = "A beginner-friendly pottery evening with local artists.",
            description = "All materials and refreshments are included. Limited seats; contact the organizer before travelling.",
            category = EventCategory.WORKSHOP,
            startsAtMillis = now + 5 * day,
            endsAtMillis = now + 5 * day + 3 * hour,
            venue = "Art House Studio",
            address = "Bejai, Mangaluru",
            distanceKm = 3.7,
            organizer = Organizer("Art House", true),
            visibility = EventVisibility.PUBLIC,
            verification = VerificationLevel.ORGANIZER,
            attendeeCount = 18,
            friendNames = listOf("Ananya"),
            themeKey = "workshop",
        ),
        Event(
            id = "private-celebration",
            title = "Aarav & Diya's Celebration",
            summary = "You were invited by Rohan.",
            description = "A private celebration shared with selected friends. Venue details are visible only to invited guests.",
            category = EventCategory.PRIVATE,
            startsAtMillis = now + 6 * day,
            endsAtMillis = now + 6 * day + 6 * hour,
            venue = "Invitation venue",
            address = "Mangaluru",
            distanceKm = 5.2,
            organizer = Organizer("Rohan", false),
            visibility = EventVisibility.INVITE_ONLY,
            verification = VerificationLevel.CONFIRMED,
            attendeeCount = 42,
            friendNames = listOf("Rohan", "Ananya", "Nisha"),
            themeKey = "private",
        ),
    )
}

private fun themeFor(category: EventCategory): String = when (category) {
    EventCategory.FESTIVAL -> "festival"
    EventCategory.SALE -> "sale"
    EventCategory.CONCERT -> "concert"
    EventCategory.COMMUNITY -> "community"
    EventCategory.SPORTS -> "sports"
    EventCategory.WORKSHOP -> "workshop"
    EventCategory.PRIVATE -> "private"
    EventCategory.ALL -> "festival"
}
