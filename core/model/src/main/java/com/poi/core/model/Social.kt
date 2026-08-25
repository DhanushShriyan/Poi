package com.poi.core.model

enum class FriendshipStatus {
    PENDING,
    ACCEPTED,
}

enum class FriendshipDirection {
    INCOMING,
    OUTGOING,
    CONNECTED,
}

data class SocialProfile(
    val id: String,
    val displayName: String,
    val handle: String,
    val homeArea: String,
)

data class Friendship(
    val id: String,
    val profile: SocialProfile,
    val status: FriendshipStatus,
    val direction: FriendshipDirection,
    val createdAtMillis: Long,
)

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
}

data class EventInvitation(
    val id: String,
    val eventId: String,
    val eventTitle: String,
    val inviter: SocialProfile,
    val invitee: SocialProfile,
    val status: InvitationStatus,
    val createdAtMillis: Long,
)

enum class SocialActivityType(val label: String) {
    INTERESTED("is interested in"),
    GOING("is going to"),
    HERE("checked in at"),
    HOSTING("is hosting"),
}

data class SocialActivity(
    val id: String,
    val actor: SocialProfile,
    val eventId: String,
    val type: SocialActivityType,
    val createdAtMillis: Long,
)

data class SocialSyncState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastSyncedAtMillis: Long? = null,
)
