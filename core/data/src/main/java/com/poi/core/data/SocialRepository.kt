package com.poi.core.data

import com.poi.core.model.EventInvitation
import com.poi.core.model.Friendship
import com.poi.core.model.SocialActivity
import com.poi.core.model.SocialProfile
import com.poi.core.model.SocialSyncState
import kotlinx.coroutines.flow.StateFlow

interface SocialRepository {
    val directory: StateFlow<List<SocialProfile>>
    val friendships: StateFlow<List<Friendship>>
    val invitations: StateFlow<List<EventInvitation>>
    val activity: StateFlow<List<SocialActivity>>
    val syncState: StateFlow<SocialSyncState>

    suspend fun refresh()
    suspend fun sendFriendRequest(profileId: String)
    suspend fun respondToFriendRequest(friendshipId: String, accept: Boolean)
    suspend fun removeFriendship(friendshipId: String)
    suspend fun inviteToEvent(eventId: String, profileId: String)
    suspend fun respondToInvitation(invitationId: String, accept: Boolean)
}

fun List<SocialProfile>.searchProfiles(query: String, currentUserId: String?): List<SocialProfile> {
    val normalized = query.trim().lowercase()
    if (normalized.length < 2) return emptyList()
    return asSequence()
        .filterNot { it.id == currentUserId }
        .filter { profile ->
            profile.displayName.lowercase().contains(normalized) ||
                profile.handle.lowercase().contains(normalized) ||
                profile.homeArea.lowercase().contains(normalized)
        }
        .sortedBy(SocialProfile::displayName)
        .take(30)
        .toList()
}
