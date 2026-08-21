package com.poi.core.data

import android.content.Context
import com.poi.core.auth.AuthRepository
import com.poi.core.cloud.PoiCloudClient
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
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(SupabaseExperimental::class)
class SupabaseEventRepository(
    context: Context,
    private val cloud: PoiCloudClient,
    private val authRepository: AuthRepository,
) : EventRepository {
    private val localPreferences = LocalEventRepository(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val reportedIds = mutableSetOf<String>()
    private val eventRows = MutableStateFlow<List<EventRow>>(emptyList())

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    override val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _allEvents = MutableStateFlow<List<Event>>(emptyList())
    override val allEvents: StateFlow<List<Event>> = _allEvents.asStateFlow()

    private val _reportedEvents = MutableStateFlow<List<EventReport>>(emptyList())
    override val reportedEvents: StateFlow<List<EventReport>> = _reportedEvents.asStateFlow()

    private val _attendance = MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())
    override val attendance: StateFlow<Map<String, AttendanceStatus>> = _attendance.asStateFlow()

    private val _checkInVisibility = MutableStateFlow<Map<String, CheckInVisibility>>(emptyMap())
    override val checkInVisibility: StateFlow<Map<String, CheckInVisibility>> =
        _checkInVisibility.asStateFlow()

    override val settings: StateFlow<AppSettings> = localPreferences.settings
    override val profile: StateFlow<UserProfile> = localPreferences.profile

    init {
        scope.launch {
            cloud.supabase.from("events")
                .selectAsFlow(EventRow::id)
                .catch { emit(emptyList()) }
                .collectLatest { rows ->
                    eventRows.value = rows
                    refreshEvents()
                }
        }
        scope.launch {
            authRepository.session.collectLatest { session ->
                if (!session.isAuthenticated) {
                    _attendance.value = emptyMap()
                    _checkInVisibility.value = emptyMap()
                } else {
                    refreshAttendance(session.user?.id)
                }
                refreshEvents()
            }
        }
    }

    override suspend fun setAttendance(
        eventId: String,
        status: AttendanceStatus,
        visibility: CheckInVisibility?,
    ) {
        val userId = requireUserId()
        if (status == AttendanceStatus.NONE) {
            cloud.supabase.from("attendance").delete {
                filter {
                    eq("user_id", userId)
                    eq("event_id", eventId)
                }
            }
            _attendance.value = _attendance.value - eventId
            _checkInVisibility.value = _checkInVisibility.value - eventId
        } else {
            val resolvedVisibility = visibility
                ?: _checkInVisibility.value[eventId]
                ?: settings.value.defaultCheckInVisibility
            cloud.supabase.from("attendance").upsert(
                AttendanceRow(
                    userId = userId,
                    eventId = eventId,
                    status = status.name.lowercase(),
                    visibility = resolvedVisibility.name.lowercase(),
                ),
            ) {
                onConflict = "user_id,event_id"
            }
            _attendance.value = _attendance.value + (eventId to status)
            _checkInVisibility.value = _checkInVisibility.value + (eventId to resolvedVisibility)
        }
    }

    override suspend fun createEvent(newEvent: NewEvent): Event {
        requireUserId()
        return cloud.supabase.from("events").insert(
            NewEventRow(
                title = newEvent.title.trim(),
                summary = newEvent.summary.trim(),
                description = newEvent.summary.trim(),
                category = newEvent.category.name.lowercase(),
                startsAtMillis = newEvent.startsAtMillis,
                endsAtMillis = newEvent.endsAtMillis,
                venue = newEvent.venue.trim(),
                address = newEvent.address.trim(),
                organizerName = newEvent.organizerName.trim(),
                visibility = newEvent.visibility.name.lowercase(),
                themeKey = themeFor(newEvent.category),
            ),
        ) { select() }
            .decodeSingle<EventRow>()
            .toModel(authRepository.session.value.user?.id)
    }

    override suspend fun reportEvent(eventId: String, reason: String) {
        val report = EventReport(
            eventId = eventId,
            reason = reason.trim(),
            reportedAtMillis = System.currentTimeMillis(),
        )
        cloud.supabase.from("reports").insert(
            ReportRow(
                reporterId = requireUserId(),
                eventId = eventId,
                reason = report.reason,
                reportedAtMillis = report.reportedAtMillis,
            ),
        )
        reportedIds += eventId
        _reportedEvents.value = (_reportedEvents.value + report)
            .sortedByDescending(EventReport::reportedAtMillis)
        refreshEvents()
    }

    override suspend fun restoreReportedEvent(eventId: String) {
        val userId = requireUserId()
        cloud.supabase.from("reports").delete {
            filter {
                eq("reporter_id", userId)
                eq("event_id", eventId)
            }
        }
        reportedIds -= eventId
        _reportedEvents.value = _reportedEvents.value.filterNot { it.eventId == eventId }
        refreshEvents()
    }

    override suspend fun updateEvent(event: Event) {
        cloud.supabase.from("events").update(event.toUpdateRow()) {
            filter { eq("id", event.id) }
        }
    }

    override suspend fun deleteEvent(eventId: String) {
        cloud.supabase.from("events").delete {
            filter { eq("id", eventId) }
        }
    }

    override suspend fun updateSettings(settings: AppSettings) {
        localPreferences.updateSettings(settings)
    }

    private suspend fun refreshAttendance(userId: String?) {
        if (userId == null) return
        val rows = runCatching {
            cloud.supabase.from("attendance").select {
                filter { eq("user_id", userId) }
            }.decodeList<AttendanceRow>()
        }.getOrDefault(emptyList())
        _attendance.value = rows.mapNotNull { row ->
            enumValueOrNull<AttendanceStatus>(row.status.uppercase())?.let { row.eventId to it }
        }.toMap()
        _checkInVisibility.value = rows.mapNotNull { row ->
            enumValueOrNull<CheckInVisibility>(row.visibility.uppercase())?.let { row.eventId to it }
        }.toMap()
    }

    private fun refreshEvents() {
        val userId = authRepository.session.value.user?.id
        val mapped = eventRows.value
            .map { it.toModel(userId) }
            .filterNot(Event::isCancelled)
            .sortedBy(Event::startsAtMillis)
        _allEvents.value = mapped
        _events.value = mapped.filterNot { it.id in reportedIds }
    }

    private fun requireUserId(): String =
        requireNotNull(authRepository.session.value.user?.id) { "Sign in to continue." }
}

@Serializable
private data class EventRow(
    val id: String,
    @SerialName("created_by") val createdBy: String? = null,
    val title: String,
    val summary: String,
    val description: String,
    val category: String,
    @SerialName("starts_at_millis") val startsAtMillis: Long,
    @SerialName("ends_at_millis") val endsAtMillis: Long,
    val venue: String,
    val address: String,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("organizer_name") val organizerName: String,
    @SerialName("organizer_verified") val organizerVerified: Boolean = false,
    val visibility: String,
    val verification: String = "community",
    @SerialName("attendee_count") val attendeeCount: Int = 0,
    @SerialName("friend_names") val friendNames: List<String> = emptyList(),
    @SerialName("theme_key") val themeKey: String,
    val featured: Boolean = false,
    @SerialName("is_cancelled") val isCancelled: Boolean = false,
    @SerialName("updated_at_millis") val updatedAtMillis: Long? = null,
)

@Serializable
private data class NewEventRow(
    val title: String,
    val summary: String,
    val description: String,
    val category: String,
    @SerialName("starts_at_millis") val startsAtMillis: Long,
    @SerialName("ends_at_millis") val endsAtMillis: Long,
    val venue: String,
    val address: String,
    @SerialName("organizer_name") val organizerName: String,
    val visibility: String,
    @SerialName("theme_key") val themeKey: String,
)

@Serializable
private data class EventUpdateRow(
    val title: String,
    val summary: String,
    val description: String,
    val category: String,
    @SerialName("starts_at_millis") val startsAtMillis: Long,
    @SerialName("ends_at_millis") val endsAtMillis: Long,
    val venue: String,
    val address: String,
    @SerialName("distance_km") val distanceKm: Double,
    @SerialName("organizer_name") val organizerName: String,
    @SerialName("organizer_verified") val organizerVerified: Boolean,
    val visibility: String,
    val verification: String,
    @SerialName("attendee_count") val attendeeCount: Int,
    @SerialName("friend_names") val friendNames: List<String>,
    @SerialName("theme_key") val themeKey: String,
    val featured: Boolean,
    @SerialName("is_cancelled") val isCancelled: Boolean,
    @SerialName("updated_at_millis") val updatedAtMillis: Long,
)

@Serializable
private data class AttendanceRow(
    @SerialName("user_id") val userId: String,
    @SerialName("event_id") val eventId: String,
    val status: String,
    val visibility: String,
)

@Serializable
private data class ReportRow(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("event_id") val eventId: String,
    val reason: String,
    @SerialName("reported_at_millis") val reportedAtMillis: Long,
)

private fun EventRow.toModel(currentUserId: String?): Event = Event(
    id = id,
    title = title,
    summary = summary,
    description = description,
    category = enumValueOrNull<EventCategory>(category.uppercase()) ?: EventCategory.COMMUNITY,
    startsAtMillis = startsAtMillis,
    endsAtMillis = endsAtMillis,
    venue = venue,
    address = address,
    distanceKm = distanceKm,
    organizer = Organizer(organizerName, organizerVerified),
    visibility = enumValueOrNull<EventVisibility>(visibility.uppercase()) ?: EventVisibility.PUBLIC,
    verification = enumValueOrNull<VerificationLevel>(verification.uppercase())
        ?: VerificationLevel.COMMUNITY,
    attendeeCount = attendeeCount,
    friendNames = friendNames,
    themeKey = themeKey,
    featured = featured,
    createdByCurrentUser = currentUserId != null && currentUserId == createdBy,
    isCancelled = isCancelled,
    updatedAtMillis = updatedAtMillis,
)

private fun Event.toUpdateRow(): EventUpdateRow = EventUpdateRow(
    title = title,
    summary = summary,
    description = description,
    category = category.name.lowercase(),
    startsAtMillis = startsAtMillis,
    endsAtMillis = endsAtMillis,
    venue = venue,
    address = address,
    distanceKm = distanceKm,
    organizerName = organizer.name,
    organizerVerified = organizer.isVerified,
    visibility = visibility.name.lowercase(),
    verification = verification.name.lowercase(),
    attendeeCount = attendeeCount,
    friendNames = friendNames,
    themeKey = themeKey,
    featured = featured,
    isCancelled = isCancelled,
    updatedAtMillis = System.currentTimeMillis(),
)

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name == value }

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
