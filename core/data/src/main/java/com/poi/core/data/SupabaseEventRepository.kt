package com.poi.core.data

import android.content.Context
import com.poi.core.auth.AuthRepository
import com.poi.core.cloud.PoiCloudClient
import com.poi.core.model.AppSettings
import com.poi.core.model.AttendanceEvidence
import com.poi.core.model.AttendanceStatus
import com.poi.core.model.AttendanceVerification
import com.poi.core.model.AttendanceVerificationMethod
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
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

    private val _events = MutableStateFlow(localPreferences.events.value)
    override val events: StateFlow<List<Event>> = _events.asStateFlow()

    private val _allEvents = MutableStateFlow(localPreferences.allEvents.value)
    override val allEvents: StateFlow<List<Event>> = _allEvents.asStateFlow()

    private val _reportedEvents = MutableStateFlow<List<EventReport>>(emptyList())
    override val reportedEvents: StateFlow<List<EventReport>> = _reportedEvents.asStateFlow()

    private val _attendance = MutableStateFlow<Map<String, AttendanceStatus>>(emptyMap())
    override val attendance: StateFlow<Map<String, AttendanceStatus>> = _attendance.asStateFlow()

    private val _attendanceVerification = MutableStateFlow<Map<String, AttendanceVerification>>(emptyMap())
    override val attendanceVerification: StateFlow<Map<String, AttendanceVerification>> =
        _attendanceVerification.asStateFlow()

    private val _checkInVisibility = MutableStateFlow<Map<String, CheckInVisibility>>(emptyMap())
    override val checkInVisibility: StateFlow<Map<String, CheckInVisibility>> =
        _checkInVisibility.asStateFlow()

    override val settings: StateFlow<AppSettings> = localPreferences.settings

    private val _profile = MutableStateFlow(localPreferences.profile.value)
    override val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _syncState = MutableStateFlow(
        DataSyncState(isCloudBacked = true, isLoading = true),
    )
    override val syncState: StateFlow<DataSyncState> = _syncState.asStateFlow()

    init {
        scope.launch {
            cloud.supabase.from("events")
                .selectAsFlow(EventRow::id)
                .onStart { markLoading() }
                .retryWhen { cause, attempt ->
                    markFailure(cause)
                    delay((attempt + 1).coerceAtMost(6) * 1_000L)
                    true
                }
                .catch { error -> markFailure(error) }
                .collectLatest { rows ->
                    eventRows.value = rows
                    markSuccess()
                    refreshEvents()
                    refreshProfileCounts()
                }
        }
        scope.launch {
            authRepository.session.collectLatest { session ->
                if (!session.isAuthenticated) {
                    _attendance.value = emptyMap()
                    _attendanceVerification.value = emptyMap()
                    _checkInVisibility.value = emptyMap()
                    reportedIds.clear()
                    _reportedEvents.value = emptyList()
                    _profile.value = localPreferences.profile.value
                } else {
                    runCatching { refreshMemberData(checkNotNull(session.user?.id)) }
                        .onFailure(::markFailure)
                }
                refreshEvents()
            }
        }
    }

    override suspend fun refresh() = connectedOperation {
        eventRows.value = cloud.supabase.from("events").select().decodeList<EventRow>()
        val userId = authRepository.session.value.user?.id
        if (userId == null) {
            _attendance.value = emptyMap()
            _attendanceVerification.value = emptyMap()
            _checkInVisibility.value = emptyMap()
            reportedIds.clear()
            _reportedEvents.value = emptyList()
        } else {
            refreshMemberData(userId)
        }
        refreshEvents()
        refreshProfileCounts()
    }

    override suspend fun setAttendance(
        eventId: String,
        status: AttendanceStatus,
        visibility: CheckInVisibility?,
        evidence: AttendanceEvidence?,
    ) = connectedOperation {
        val userId = requireUserId()
        if (status == AttendanceStatus.NONE) {
            cloud.supabase.from("attendance").delete {
                filter {
                    eq("user_id", userId)
                    eq("event_id", eventId)
                }
            }
            _attendance.value = _attendance.value - eventId
            _attendanceVerification.value = _attendanceVerification.value - eventId
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
                    checkInLatitude = evidence?.latitude,
                    checkInLongitude = evidence?.longitude,
                    accuracyMeters = evidence?.accuracyMeters?.toInt(),
                ),
            ) {
                onConflict = "user_id,event_id"
            }
            _attendance.value = _attendance.value + (eventId to status)
            _checkInVisibility.value = _checkInVisibility.value + (eventId to resolvedVisibility)
            refreshAttendance(userId)
        }
        refreshProfileCounts()
    }

    override suspend fun createEvent(newEvent: NewEvent): Event = connectedOperation {
        requireUserId()
        val createdRow = cloud.supabase.from("events").insert(
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
                latitude = newEvent.latitude,
                longitude = newEvent.longitude,
                checkInRadiusMeters = newEvent.checkInRadiusMeters,
            ),
        ) { select() }
            .decodeSingle<EventRow>()
        eventRows.value = (eventRows.value.filterNot { it.id == createdRow.id } + createdRow)
            .sortedBy(EventRow::startsAtMillis)
        refreshEvents()
        refreshProfileCounts()
        createdRow.toModel(authRepository.session.value.user?.id)
    }

    override suspend fun reportEvent(eventId: String, reason: String) = connectedOperation {
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

    override suspend fun restoreReportedEvent(eventId: String) = connectedOperation {
        val userId = requireUserId()
        cloud.supabase.from("reports").delete {
            filter {
                eq("event_id", eventId)
                if (!authRepository.session.value.isAdmin) eq("reporter_id", userId)
            }
        }
        reportedIds -= eventId
        _reportedEvents.value = _reportedEvents.value.filterNot { it.eventId == eventId }
        refreshEvents()
    }

    override suspend fun updateEvent(event: Event) = connectedOperation {
        cloud.supabase.from("events").update(event.toUpdateRow()) {
            filter { eq("id", event.id) }
        }
        Unit
    }

    override suspend fun deleteEvent(eventId: String) = connectedOperation {
        cloud.supabase.from("events").delete {
            filter { eq("id", eventId) }
        }
        Unit
    }

    override suspend fun updateProfile(displayName: String, handle: String, homeArea: String) =
        connectedOperation {
            val update = normalizeProfileUpdate(displayName, handle, homeArea)
            val userId = requireUserId()
            cloud.supabase.from("profiles").update(update) {
                filter { eq("id", userId) }
            }
            refreshProfile(userId)
        }

    override suspend fun updateSettings(settings: AppSettings) {
        localPreferences.updateSettings(settings)
        val userId = authRepository.session.value.user?.id ?: return
        connectedOperation {
            cloud.supabase.from("profiles").update(
                ProfilePrivacyUpdateRow(
                    sharePlansToFriends = settings.showPlansToFriends,
                    shareFriendActivity = settings.friendActivity,
                ),
            ) {
                filter { eq("id", userId) }
            }
        }
    }

    private suspend fun refreshMemberData(userId: String) {
        refreshAttendance(userId)
        refreshReports()
        refreshProfile(userId)
    }

    private suspend fun refreshAttendance(userId: String?) {
        if (userId == null) return
        val rows = cloud.supabase.from("attendance").select {
            filter { eq("user_id", userId) }
        }.decodeList<AttendanceRow>()
        _attendance.value = rows.mapNotNull { row ->
            enumValueOrNull<AttendanceStatus>(row.status.uppercase())?.let { row.eventId to it }
        }.toMap()
        _checkInVisibility.value = rows.mapNotNull { row ->
            enumValueOrNull<CheckInVisibility>(row.visibility.uppercase())?.let { row.eventId to it }
        }.toMap()
        _attendanceVerification.value = rows.mapNotNull { row ->
            if (row.status !in setOf("here", "attended")) return@mapNotNull null
            val method = if (row.verificationMethod == "proximity") {
                AttendanceVerificationMethod.PROXIMITY
            } else {
                AttendanceVerificationMethod.MANUAL
            }
            row.eventId to AttendanceVerification(
                method = method,
                distanceMeters = row.distanceMeters,
                accuracyMeters = row.accuracyMeters,
                verifiedAtMillis = row.verifiedAtMillis,
            )
        }.toMap()
    }

    private suspend fun refreshReports() {
        val rows = cloud.supabase.from("reports").select().decodeList<ReportRow>()
        reportedIds.clear()
        reportedIds += rows.map(ReportRow::eventId)
        _reportedEvents.value = rows.map { row ->
            EventReport(row.eventId, row.reason, row.reportedAtMillis)
        }.sortedByDescending(EventReport::reportedAtMillis)
    }

    private suspend fun refreshProfile(userId: String) {
        val row = cloud.supabase.from("profiles").select {
            filter { eq("id", userId) }
        }.decodeSingle<ProfileRow>()
        _profile.value = UserProfile(
            id = row.id,
            displayName = row.displayName,
            handle = row.handle ?: defaultHandle(row.id),
            homeArea = row.homeArea,
            attendedCount = 0,
            hostedCount = 0,
            contributionPoints = 0,
        )
        localPreferences.updateSettings(
            settings.value.copy(
                showPlansToFriends = row.sharePlansToFriends,
                friendActivity = row.shareFriendActivity,
            ),
        )
        refreshProfileCounts()
    }

    private fun refreshProfileCounts() {
        val userId = authRepository.session.value.user?.id ?: return
        _profile.value = _profile.value.copy(
            attendedCount = _attendance.value.values.count {
                it == AttendanceStatus.HERE || it == AttendanceStatus.ATTENDED
            },
            hostedCount = eventRows.value.count { it.createdBy == userId },
        )
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

    private suspend fun <T> connectedOperation(block: suspend () -> T): T {
        markLoading()
        return try {
            block().also { markSuccess() }
        } catch (error: Throwable) {
            markFailure(error)
            throw error
        }
    }

    private fun markLoading() {
        _syncState.value = _syncState.value.copy(isLoading = true, errorMessage = null)
    }

    private fun markSuccess() {
        _syncState.value = DataSyncState(
            isCloudBacked = true,
            isConnected = true,
            lastSyncedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun markFailure(error: Throwable) {
        _syncState.value = _syncState.value.copy(
            isLoading = false,
            isConnected = false,
            errorMessage = serviceErrorMessage(error),
        )
    }
}

internal fun serviceErrorMessage(error: Throwable): String {
    val message = error.message.orEmpty()
    val normalized = message.lowercase()
    return when {
        normalized.contains("unable to resolve host") ||
            normalized.contains("no address associated with hostname") ||
            normalized.contains("failed to connect") ||
            normalized.contains("timeout") ->
            "Poi could not reach the service. Check your connection and retry."
        normalized.contains("row-level security") || normalized.contains("permission denied") ->
            "Your account does not have permission to make that change. Please sign in again."
        message.isBlank() -> "Poi could not complete that request. Please try again."
        else -> message.lineSequence().first().take(180)
    }
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("check_in_radius_meters") val checkInRadiusMeters: Int = 500,
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("check_in_radius_meters") val checkInRadiusMeters: Int,
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
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("check_in_radius_meters") val checkInRadiusMeters: Int,
)

@Serializable
private data class AttendanceRow(
    @SerialName("user_id") val userId: String,
    @SerialName("event_id") val eventId: String,
    val status: String,
    val visibility: String,
    @SerialName("check_in_latitude") val checkInLatitude: Double? = null,
    @SerialName("check_in_longitude") val checkInLongitude: Double? = null,
    @SerialName("accuracy_meters") val accuracyMeters: Int? = null,
    @SerialName("verification_method") val verificationMethod: String = "manual",
    @SerialName("distance_meters") val distanceMeters: Int? = null,
    @SerialName("verified_at_millis") val verifiedAtMillis: Long? = null,
)

@Serializable
private data class ReportRow(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("event_id") val eventId: String,
    val reason: String,
    @SerialName("reported_at_millis") val reportedAtMillis: Long,
)

@Serializable
private data class ProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val handle: String? = null,
    @SerialName("home_area") val homeArea: String,
    @SerialName("share_plans_to_friends") val sharePlansToFriends: Boolean = true,
    @SerialName("share_friend_activity") val shareFriendActivity: Boolean = true,
)

@Serializable
private data class ProfilePrivacyUpdateRow(
    @SerialName("share_plans_to_friends") val sharePlansToFriends: Boolean,
    @SerialName("share_friend_activity") val shareFriendActivity: Boolean,
)

@Serializable
internal data class ProfileUpdateRow(
    @SerialName("display_name") val displayName: String,
    val handle: String,
    @SerialName("home_area") val homeArea: String,
)

internal fun normalizeProfileUpdate(
    displayName: String,
    handle: String,
    homeArea: String,
): ProfileUpdateRow {
    val cleanName = displayName.trim()
    val cleanHandle = handle.trim().removePrefix("@").lowercase()
    val cleanHomeArea = homeArea.trim()
    require(cleanName.length in 1..60) { "Enter a name between 1 and 60 characters." }
    require(cleanHandle.matches(Regex("[a-z0-9_]{3,24}"))) {
        "Handle must be 3–24 letters, numbers, or underscores."
    }
    require(cleanHomeArea.length in 1..100) { "Enter your city or home area." }
    return ProfileUpdateRow(cleanName, "@$cleanHandle", cleanHomeArea)
}

private fun defaultHandle(userId: String): String = "@${userId.replace("-", "").take(12)}"

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
    latitude = latitude,
    longitude = longitude,
    checkInRadiusMeters = checkInRadiusMeters,
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
    latitude = latitude,
    longitude = longitude,
    checkInRadiusMeters = checkInRadiusMeters,
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
