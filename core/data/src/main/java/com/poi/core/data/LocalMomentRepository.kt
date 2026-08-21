package com.poi.core.data

import com.poi.core.model.EventMoment
import com.poi.core.model.MomentComment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalMomentRepository : MomentRepository {
    private val _moments = MutableStateFlow<List<EventMoment>>(emptyList())
    override val moments: StateFlow<List<EventMoment>> = _moments.asStateFlow()

    private val _comments = MutableStateFlow<List<MomentComment>>(emptyList())
    override val comments: StateFlow<List<MomentComment>> = _comments.asStateFlow()

    override val isCloudBacked: Boolean = false

    override suspend fun refresh() = Unit

    override suspend fun createMoment(
        eventId: String,
        image: ByteArray,
        mimeType: String,
        caption: String,
    ): Nothing = cloudRequired()

    override suspend fun toggleLike(moment: EventMoment): Nothing = cloudRequired()

    override suspend fun addComment(momentId: String, body: String): Nothing = cloudRequired()

    override suspend fun recordView(momentId: String) = Unit

    override suspend fun createShareUrl(moment: EventMoment): String = moment.imageUrl

    override suspend fun deleteMoment(moment: EventMoment): Nothing = cloudRequired()

    private fun cloudRequired(): Nothing =
        error("Event Moments requires Poi's live cloud connection.")
}
