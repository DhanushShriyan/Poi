package com.poi.feature.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.data.EventRepository
import com.poi.core.data.SocialRepository
import com.poi.core.data.searchProfiles
import com.poi.core.designsystem.PoiInitialAvatar
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.model.EventInvitation
import com.poi.core.model.Friendship
import com.poi.core.model.FriendshipDirection
import com.poi.core.model.FriendshipStatus
import com.poi.core.model.InvitationStatus
import com.poi.core.model.SocialProfile
import kotlinx.coroutines.launch

private enum class SocialTab(val label: String) {
    FRIENDS("Friends"),
    REQUESTS("Requests"),
    INVITES("Invites"),
    ACTIVITY("Activity"),
}

@Composable
fun SocialScreen(
    socialRepository: SocialRepository,
    eventRepository: EventRepository,
    currentUserId: String,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val directory by socialRepository.directory.collectAsStateWithLifecycle()
    val friendships by socialRepository.friendships.collectAsStateWithLifecycle()
    val invitations by socialRepository.invitations.collectAsStateWithLifecycle()
    val activity by socialRepository.activity.collectAsStateWithLifecycle()
    val syncState by socialRepository.syncState.collectAsStateWithLifecycle()
    val events by eventRepository.events.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(SocialTab.FRIENDS) }
    var query by remember { mutableStateOf("") }
    var busyId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val eventNames = remember(events) { events.associate { it.id to it.title } }
    val relationshipByProfile = remember(friendships) {
        friendships.associateBy { it.profile.id }
    }
    val searchResults = remember(directory, query, currentUserId) {
        directory.searchProfiles(query, currentUserId)
    }

    fun runAction(id: String, action: suspend () -> Unit) {
        busyId = id
        message = null
        scope.launch {
            runCatching { action() }
                .onFailure { message = it.message ?: "That update could not be saved." }
            busyId = null
        }
    }

    LaunchedEffect(currentUserId) {
        runCatching { socialRepository.refresh() }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Your people", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Plan together without turning every event into a popularity contest.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.take(60) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                placeholder = { Text("Find people by name, handle or area") },
            )
        }

        if (query.isNotBlank()) {
            item { PoiSectionHeader("People") }
            if (query.trim().length < 2) {
                item { HintCard("Type at least two characters to search.") }
            } else if (searchResults.isEmpty() && !syncState.isLoading) {
                item { HintCard("No people match that search yet.") }
            } else {
                items(searchResults, key = SocialProfile::id) { profile ->
                    val relationship = relationshipByProfile[profile.id]
                    PersonCard(
                        profile = profile,
                        relationship = relationship,
                        busy = busyId == profile.id,
                        onConnect = {
                            runAction(profile.id) { socialRepository.sendFriendRequest(profile.id) }
                        },
                    )
                }
            }
        } else {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SocialTab.entries) { item ->
                        val badge = when (item) {
                            SocialTab.REQUESTS -> friendships.count {
                                it.direction == FriendshipDirection.INCOMING &&
                                    it.status == FriendshipStatus.PENDING
                            }
                            SocialTab.INVITES -> invitations.count {
                                it.invitee.id == currentUserId && it.status == InvitationStatus.PENDING
                            }
                            else -> 0
                        }
                        FilterChip(
                            selected = tab == item,
                            onClick = { tab = item },
                            label = { Text(item.label + if (badge > 0) "  $badge" else "") },
                        )
                    }
                }
            }

            message?.let { text ->
                item { Text(text, color = MaterialTheme.colorScheme.error) }
            }

            when (tab) {
                SocialTab.FRIENDS -> {
                    val friends = friendships.filter { it.status == FriendshipStatus.ACCEPTED }
                    if (friends.isEmpty()) {
                        item { EmptySocialCard(Icons.Default.Groups, "Your friend circle starts here", "Search for someone you know and send a request.") }
                    } else {
                        items(friends, key = Friendship::id) { friendship ->
                            FriendCard(
                                friendship = friendship,
                                busy = busyId == friendship.id,
                                onRemove = {
                                    runAction(friendship.id) {
                                        socialRepository.removeFriendship(friendship.id)
                                    }
                                },
                            )
                        }
                    }
                }

                SocialTab.REQUESTS -> {
                    val requests = friendships.filter { it.status == FriendshipStatus.PENDING }
                    if (requests.isEmpty()) {
                        item { EmptySocialCard(Icons.Default.GroupAdd, "No pending requests", "New requests and sent connections will appear here live.") }
                    } else {
                        items(requests, key = Friendship::id) { friendship ->
                            RequestCard(
                                friendship = friendship,
                                busy = busyId == friendship.id,
                                onAccept = {
                                    runAction(friendship.id) {
                                        socialRepository.respondToFriendRequest(friendship.id, true)
                                    }
                                },
                                onDecline = {
                                    runAction(friendship.id) {
                                        if (friendship.direction == FriendshipDirection.INCOMING) {
                                            socialRepository.respondToFriendRequest(friendship.id, false)
                                        } else {
                                            socialRepository.removeFriendship(friendship.id)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }

                SocialTab.INVITES -> {
                    val visibleInvites = invitations.filter { it.status == InvitationStatus.PENDING }
                    if (visibleInvites.isEmpty()) {
                        item { EmptySocialCard(Icons.Default.Inbox, "No event invitations", "Private and friend invitations will stay organized here.") }
                    } else {
                        items(visibleInvites, key = EventInvitation::id) { invitation ->
                            InvitationCard(
                                invitation = invitation,
                                currentUserId = currentUserId,
                                eventTitle = eventNames[invitation.eventId] ?: invitation.eventTitle,
                                busy = busyId == invitation.id,
                                onOpen = { onEventClick(invitation.eventId) },
                                onAccept = {
                                    runAction(invitation.id) {
                                        socialRepository.respondToInvitation(invitation.id, true)
                                    }
                                },
                                onDecline = {
                                    runAction(invitation.id) {
                                        socialRepository.respondToInvitation(invitation.id, false)
                                    }
                                },
                            )
                        }
                    }
                }

                SocialTab.ACTIVITY -> {
                    if (activity.isEmpty()) {
                        item { EmptySocialCard(Icons.Default.Groups, "Quiet for now", "Opted-in plans from accepted friends will appear here.") }
                    } else {
                        items(activity, key = { it.id }) { item ->
                            Card(
                                onClick = { onEventClick(item.eventId) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    PoiInitialAvatar(item.actor.displayName, Modifier.size(44.dp))
                                    Spacer(Modifier.size(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(item.actor.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            "${item.type.label} ${eventNames[item.eventId] ?: "an event"}",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonCard(
    profile: SocialProfile,
    relationship: Friendship?,
    busy: Boolean,
    onConnect: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PoiInitialAvatar(profile.displayName, Modifier.size(44.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(profile.displayName, style = MaterialTheme.typography.titleMedium)
                Text("${profile.handle} · ${profile.homeArea}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onConnect, enabled = relationship == null && !busy) {
                Text(
                    when {
                        relationship?.status == FriendshipStatus.ACCEPTED -> "Friends"
                        relationship != null -> "Pending"
                        busy -> "Sending…"
                        else -> "Connect"
                    },
                )
            }
        }
    }
}

@Composable
private fun FriendCard(friendship: Friendship, busy: Boolean, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PoiInitialAvatar(friendship.profile.displayName, Modifier.size(44.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(friendship.profile.displayName, style = MaterialTheme.typography.titleMedium)
                Text(friendship.profile.handle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onRemove, enabled = !busy) { Text(if (busy) "Removing…" else "Remove") }
        }
    }
}

@Composable
private fun RequestCard(
    friendship: Friendship,
    busy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PoiInitialAvatar(friendship.profile.displayName, Modifier.size(44.dp))
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(friendship.profile.displayName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (friendship.direction == FriendshipDirection.INCOMING) "Wants to connect" else "Request sent",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (friendship.direction == FriendshipDirection.INCOMING) {
                    Button(onClick = onAccept, enabled = !busy) { Text("Accept") }
                }
                OutlinedButton(onClick = onDecline, enabled = !busy) {
                    Text(if (friendship.direction == FriendshipDirection.INCOMING) "Decline" else "Cancel request")
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(
    invitation: EventInvitation,
    currentUserId: String,
    eventTitle: String,
    busy: Boolean,
    onOpen: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val incoming = invitation.invitee.id == currentUserId
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(eventTitle, style = MaterialTheme.typography.titleLarge)
            Text(
                if (incoming) "Invited by ${invitation.inviter.displayName}" else "Sent to ${invitation.invitee.displayName}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen) { Text("View event") }
                if (incoming) {
                    Button(onClick = onAccept, enabled = !busy) { Text("Accept") }
                    TextButton(onClick = onDecline, enabled = !busy) { Text("Decline") }
                }
            }
        }
    }
}

@Composable
private fun EmptySocialCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HintCard(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Text(text, Modifier.fillMaxWidth().padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
