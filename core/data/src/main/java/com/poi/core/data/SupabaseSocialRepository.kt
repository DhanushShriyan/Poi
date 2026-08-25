package com.poi.core.data

import com.poi.core.auth.AuthRepository
import com.poi.core.cloud.PoiCloudClient
import com.poi.core.model.EventInvitation
import com.poi.core.model.Friendship
import com.poi.core.model.FriendshipDirection
import com.poi.core.model.FriendshipStatus
import com.poi.core.model.InvitationStatus
import com.poi.core.model.SocialActivity
import com.poi.core.model.SocialActivityType
import com.poi.core.model.SocialProfile
import com.poi.core.model.SocialSyncState
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.selectAsFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
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
class SupabaseSocialRepository(
    private val cloud: PoiCloudClient,
    private val authRepository: AuthRepository,
) : SocialRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val directoryRows = MutableStateFlow<List<PublicProfileRow>>(emptyList())
    private val friendshipRows = MutableStateFlow<List<FriendshipRow>>(emptyList())
    private val invitationRows = MutableStateFlow<List<InvitationRow>>(emptyList())
    private val activityRows = MutableStateFlow<List<ActivityRow>>(emptyList())

    private val _directory = MutableStateFlow<List<SocialProfile>>(emptyList())
    override val directory: StateFlow<List<SocialProfile>> = _directory.asStateFlow()

    private val _friendships = MutableStateFlow<List<Friendship>>(emptyList())
    override val friendships: StateFlow<List<Friendship>> = _friendships.asStateFlow()

    private val _invitations = MutableStateFlow<List<EventInvitation>>(emptyList())
    override val invitations: StateFlow<List<EventInvitation>> = _invitations.asStateFlow()

    private val _activity = MutableStateFlow<List<SocialActivity>>(emptyList())
    override val activity: StateFlow<List<SocialActivity>> = _activity.asStateFlow()

    private val _syncState = MutableStateFlow(SocialSyncState(isLoading = false))
    override val syncState: StateFlow<SocialSyncState> = _syncState.asStateFlow()

    init {
        scope.launch {
            authRepository.session.collectLatest { session ->
                clear()
                if (!session.isAuthenticated) return@collectLatest
                coroutineScope {
                    launch {
                        cloud.supabase.from("public_profile_directory")
                            .selectAsFlow(PublicProfileRow::id)
                            .resilientRealtime()
                            .collectLatest { rows ->
                                directoryRows.value = rows
                                rebuild()
                                markSuccess()
                            }
                    }
                    launch {
                        cloud.supabase.from("friendships")
                            .selectAsFlow(FriendshipRow::id)
                            .resilientRealtime()
                            .collectLatest { rows ->
                                friendshipRows.value = rows
                                rebuild()
                                markSuccess()
                            }
                    }
                    launch {
                        cloud.supabase.from("event_invitations")
                            .selectAsFlow(InvitationRow::id)
                            .resilientRealtime()
                            .collectLatest { rows ->
                                invitationRows.value = rows
                                rebuild()
                                markSuccess()
                            }
                    }
                    launch {
                        cloud.supabase.from("social_activity")
                            .selectAsFlow(ActivityRow::id)
                            .resilientRealtime()
                            .collectLatest { rows ->
                                activityRows.value = rows
                                rebuild()
                                markSuccess()
                            }
                    }
                }
            }
        }
    }

    override suspend fun refresh() = connectedOperation {
        directoryRows.value = cloud.supabase.from("public_profile_directory")
            .select().decodeList<PublicProfileRow>()
        friendshipRows.value = cloud.supabase.from("friendships")
            .select().decodeList<FriendshipRow>()
        invitationRows.value = cloud.supabase.from("event_invitations")
            .select().decodeList<InvitationRow>()
        activityRows.value = cloud.supabase.from("social_activity")
            .select().decodeList<ActivityRow>()
        rebuild()
    }

    override suspend fun sendFriendRequest(profileId: String) = connectedOperation {
        val userId = requireUserId()
        require(profileId != userId) { "You cannot send a friend request to yourself." }
        cloud.supabase.from("friendships").insert(
            NewFriendshipRow(requesterId = userId, addresseeId = profileId),
        )
        Unit
    }

    override suspend fun respondToFriendRequest(friendshipId: String, accept: Boolean) =
        connectedOperation {
            if (accept) {
                cloud.supabase.from("friendships").update(StatusUpdateRow("accepted")) {
                    filter { eq("id", friendshipId) }
                }
            } else {
                cloud.supabase.from("friendships").delete {
                    filter { eq("id", friendshipId) }
                }
            }
            Unit
        }

    override suspend fun removeFriendship(friendshipId: String) = connectedOperation {
        cloud.supabase.from("friendships").delete {
            filter { eq("id", friendshipId) }
        }
        Unit
    }

    override suspend fun inviteToEvent(eventId: String, profileId: String) = connectedOperation {
        cloud.supabase.from("event_invitations").insert(
            NewInvitationRow(
                eventId = eventId,
                inviterId = requireUserId(),
                inviteeId = profileId,
            ),
        )
        Unit
    }

    override suspend fun respondToInvitation(invitationId: String, accept: Boolean) =
        connectedOperation {
            if (accept) {
                cloud.supabase.from("event_invitations").update(StatusUpdateRow("accepted")) {
                    filter { eq("id", invitationId) }
                }
            } else {
                cloud.supabase.from("event_invitations").delete {
                    filter { eq("id", invitationId) }
                }
            }
            Unit
        }

    private fun rebuild() {
        val userId = authRepository.session.value.user?.id ?: return clear()
        val profiles = directoryRows.value.associate { row -> row.id to row.toModel() }
        _directory.value = profiles.values.sortedBy(SocialProfile::displayName)
        _friendships.value = friendshipRows.value.mapNotNull { row ->
            val otherId = if (row.requesterId == userId) row.addresseeId else row.requesterId
            val profile = profiles[otherId] ?: return@mapNotNull null
            val accepted = row.status == "accepted"
            Friendship(
                id = row.id,
                profile = profile,
                status = if (accepted) FriendshipStatus.ACCEPTED else FriendshipStatus.PENDING,
                direction = when {
                    accepted -> FriendshipDirection.CONNECTED
                    row.addresseeId == userId -> FriendshipDirection.INCOMING
                    else -> FriendshipDirection.OUTGOING
                },
                createdAtMillis = row.createdAtMillis,
            )
        }.sortedBy { it.profile.displayName }
        _invitations.value = invitationRows.value.mapNotNull { row ->
            val inviter = profiles[row.inviterId] ?: return@mapNotNull null
            val invitee = profiles[row.inviteeId] ?: return@mapNotNull null
            EventInvitation(
                id = row.id,
                eventId = row.eventId,
                eventTitle = row.eventTitle,
                inviter = inviter,
                invitee = invitee,
                status = enumValueOrNull<InvitationStatus>(row.status.uppercase())
                    ?: InvitationStatus.PENDING,
                createdAtMillis = row.createdAtMillis,
            )
        }.sortedByDescending(EventInvitation::createdAtMillis)
        _activity.value = activityRows.value.mapNotNull { row ->
            val actor = profiles[row.actorId] ?: return@mapNotNull null
            val type = enumValueOrNull<SocialActivityType>(row.activityType.uppercase())
                ?: return@mapNotNull null
            SocialActivity(row.id, actor, row.eventId, type, row.createdAtMillis)
        }.sortedByDescending(SocialActivity::createdAtMillis)
    }

    private fun clear() {
        directoryRows.value = emptyList()
        friendshipRows.value = emptyList()
        invitationRows.value = emptyList()
        activityRows.value = emptyList()
        _directory.value = emptyList()
        _friendships.value = emptyList()
        _invitations.value = emptyList()
        _activity.value = emptyList()
    }

    private suspend fun <T> connectedOperation(block: suspend () -> T): T {
        _syncState.value = _syncState.value.copy(isLoading = true, errorMessage = null)
        return try {
            block().also { markSuccess() }
        } catch (error: Throwable) {
            markFailure(error)
            throw error
        }
    }

    private fun markSuccess() {
        _syncState.value = SocialSyncState(lastSyncedAtMillis = System.currentTimeMillis())
    }

    private fun markFailure(error: Throwable) {
        _syncState.value = SocialSyncState(
            errorMessage = error.message?.takeIf(String::isNotBlank)
                ?: "Your social updates could not be loaded.",
            lastSyncedAtMillis = _syncState.value.lastSyncedAtMillis,
        )
    }

    private fun requireUserId(): String =
        requireNotNull(authRepository.session.value.user?.id) { "Sign in to continue." }

    private fun <T> kotlinx.coroutines.flow.Flow<List<T>>.resilientRealtime() =
        onStart { _syncState.value = _syncState.value.copy(isLoading = true, errorMessage = null) }
            .retryWhen { cause, attempt ->
                markFailure(cause)
                delay((attempt + 1).coerceAtMost(6) * 1_000L)
                true
            }
            .catch { error -> markFailure(error) }
}

@Serializable
private data class PublicProfileRow(
    val id: String,
    @SerialName("display_name") val displayName: String,
    val handle: String,
    @SerialName("home_area") val homeArea: String,
) {
    fun toModel() = SocialProfile(id, displayName, handle, homeArea)
}

@Serializable
private data class FriendshipRow(
    val id: String,
    @SerialName("requester_id") val requesterId: String,
    @SerialName("addressee_id") val addresseeId: String,
    val status: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
)

@Serializable
private data class NewFriendshipRow(
    @SerialName("requester_id") val requesterId: String,
    @SerialName("addressee_id") val addresseeId: String,
)

@Serializable
private data class InvitationRow(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("event_title") val eventTitle: String,
    @SerialName("inviter_id") val inviterId: String,
    @SerialName("invitee_id") val inviteeId: String,
    val status: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
)

@Serializable
private data class NewInvitationRow(
    @SerialName("event_id") val eventId: String,
    @SerialName("inviter_id") val inviterId: String,
    @SerialName("invitee_id") val inviteeId: String,
)

@Serializable
private data class ActivityRow(
    val id: String,
    @SerialName("actor_id") val actorId: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("activity_type") val activityType: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
)

@Serializable
private data class StatusUpdateRow(val status: String)

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name == value }
