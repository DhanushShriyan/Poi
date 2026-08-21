package com.poi.core.data

import com.poi.core.model.EventMoment
import com.poi.core.model.MomentComment
import kotlinx.coroutines.flow.StateFlow

interface MomentRepository {
    val moments: StateFlow<List<EventMoment>>
    val comments: StateFlow<List<MomentComment>>
    val isCloudBacked: Boolean

    suspend fun refresh()
    suspend fun createMoment(eventId: String, image: ByteArray, mimeType: String, caption: String)
    suspend fun toggleLike(moment: EventMoment)
    suspend fun addComment(momentId: String, body: String)
    suspend fun recordView(momentId: String)
    suspend fun createShareUrl(moment: EventMoment): String
    suspend fun deleteMoment(moment: EventMoment)
}
