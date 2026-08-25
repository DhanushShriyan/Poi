package com.poi.core.data

import com.poi.core.auth.AuthRepository
import com.poi.core.model.EventInvitation
import com.poi.core.model.Friendship
import com.poi.core.model.FriendshipDirection
import com.poi.core.model.FriendshipStatus
import com.poi.core.model.InvitationStatus
import com.poi.core.model.SocialActivity
import com.poi.core.model.SocialActivityType
import com.poi.core.model.SocialProfile
import com.poi.core.model.SocialSyncState
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LocalSocialRepository(
    private val authRepository: AuthRepository,
) : SocialRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val seedDirectory = listOf(
        SocialProfile("preview-ananya", "Ananya Rao", "@ananyarao", "Mangaluru"),
        SocialProfile("preview-rohan", "Rohan Shetty", "@rohanshetty", "Mangaluru"),
        SocialProfile("preview-meera", "Meera Nair", "@meeran", "Udupi"),
        SocialProfile("preview-arjun", "Arjun Pai", "@arjunpai", "Bengaluru"),
    )

    private val _directory = MutableStateFlow(seedDirectory)
    override val directory: StateFlow<List<SocialProfile>> = _directory.asStateFlow()

    private val _friendships = MutableStateFlow(
        listOf(
            Friendship(
                id = "preview-friend-ananya",
                profile = seedDirectory[0],
                status = FriendshipStatus.ACCEPTED,
                direction = FriendshipDirection.CONNECTED,
                createdAtMillis = System.currentTimeMillis(),
            ),
            Friendship(
                id = "preview-request-rohan",
                profile = seedDirectory[1],
                status = FriendshipStatus.PENDING,
                direction = FriendshipDirection.INCOMING,
                createdAtMillis = System.currentTimeMillis(),
            ),
        ),
    )
    override val friendships: StateFlow<List<Friendship>> = _friendships.asStateFlow()

    private val _invitations = MutableStateFlow<List<EventInvitation>>(emptyList())
    override val invitations: StateFlow<List<EventInvitation>> = _invitations.asStateFlow()

    private val _activity = MutableStateFlow(
        listOf(
            SocialActivity(
                id = "preview-activity-1",
                actor = seedDirectory[0],
                eventId = "coastal-concert",
                type = SocialActivityType.GOING,
                createdAtMillis = System.currentTimeMillis(),
            ),
        ),
    )
    override val activity: StateFlow<List<SocialActivity>> = _activity.asStateFlow()

    private val _syncState = MutableStateFlow(SocialSyncState(lastSyncedAtMillis = System.currentTimeMillis()))
    override val syncState: StateFlow<SocialSyncState> = _syncState.asStateFlow()

    init {
        scope.launch {
            authRepository.session.collectLatest { session ->
                val user = session.user
                _directory.value = if (user == null) {
                    seedDirectory
                } else {
                    listOf(
                        SocialProfile(user.id, user.displayName, "@you", "Your area"),
                    ) + seedDirectory
                }
            }
        }
    }

    override suspend fun refresh() {
        _syncState.value = SocialSyncState(lastSyncedAtMillis = System.currentTimeMillis())
    }

    override suspend fun sendFriendRequest(profileId: String) {
        if (_friendships.value.any { it.profile.id == profileId }) return
        val profile = requireNotNull(_directory.value.firstOrNull { it.id == profileId })
        _friendships.value = _friendships.value + Friendship(
            id = UUID.randomUUID().toString(),
            profile = profile,
            status = FriendshipStatus.PENDING,
            direction = FriendshipDirection.OUTGOING,
            createdAtMillis = System.currentTimeMillis(),
        )
    }

    override suspend fun respondToFriendRequest(friendshipId: String, accept: Boolean) {
        _friendships.value = if (accept) {
            _friendships.value.map { friendship ->
                if (friendship.id == friendshipId) {
                    friendship.copy(
                        status = FriendshipStatus.ACCEPTED,
                        direction = FriendshipDirection.CONNECTED,
                    )
                } else {
                    friendship
                }
            }
        } else {
            _friendships.value.filterNot { it.id == friendshipId }
        }
    }

    override suspend fun removeFriendship(friendshipId: String) {
        _friendships.value = _friendships.value.filterNot { it.id == friendshipId }
    }

    override suspend fun inviteToEvent(eventId: String, profileId: String) {
        val currentUser = requireNotNull(authRepository.session.value.user)
        val invitee = requireNotNull(_directory.value.firstOrNull { it.id == profileId })
        if (_invitations.value.any { it.eventId == eventId && it.invitee.id == profileId }) return
        _invitations.value = _invitations.value + EventInvitation(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            eventTitle = "Event invitation",
            inviter = SocialProfile(currentUser.id, currentUser.displayName, "@you", "Your area"),
            invitee = invitee,
            status = InvitationStatus.PENDING,
            createdAtMillis = System.currentTimeMillis(),
        )
    }

    override suspend fun respondToInvitation(invitationId: String, accept: Boolean) {
        _invitations.value = if (accept) {
            _invitations.value.map { invitation ->
                if (invitation.id == invitationId) invitation.copy(status = InvitationStatus.ACCEPTED)
                else invitation
            }
        } else {
            _invitations.value.filterNot { it.id == invitationId }
        }
    }
}
