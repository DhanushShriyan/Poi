package com.poi.core.data

import com.poi.core.auth.AuthRepository
import com.poi.core.cloud.PoiCloudClient
import com.poi.core.model.EventMoment
import com.poi.core.model.MomentComment
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.selectAsFlow
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.util.UUID
import kotlin.time.Duration.Companion.hours
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
class SupabaseMomentRepository(
    private val cloud: PoiCloudClient,
    private val authRepository: AuthRepository,
) : MomentRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val momentRows = MutableStateFlow<List<MomentRow>>(emptyList())
    private val likedMomentIds = MutableStateFlow<Set<String>>(emptySet())
    private val viewedMomentIds = mutableSetOf<String>()

    private val _moments = MutableStateFlow<List<EventMoment>>(emptyList())
    override val moments: StateFlow<List<EventMoment>> = _moments.asStateFlow()

    private val _comments = MutableStateFlow<List<MomentComment>>(emptyList())
    override val comments: StateFlow<List<MomentComment>> = _comments.asStateFlow()

    override val isCloudBacked: Boolean = true

    init {
        scope.launch {
            cloud.supabase.from("event_moments")
                .selectAsFlow(MomentRow::id)
                .catch { emit(emptyList()) }
                .collectLatest { rows ->
                    momentRows.value = rows
                    publishMoments(rows)
                }
        }
        scope.launch {
            cloud.supabase.from("moment_comments")
                .selectAsFlow(CommentRow::id)
                .catch { emit(emptyList()) }
                .collectLatest(::publishComments)
        }
        scope.launch {
            authRepository.session.collectLatest {
                refreshLikes()
                publishMoments(momentRows.value)
            }
        }
    }

    override suspend fun refresh() {
        val rows = cloud.supabase.from("event_moments").select().decodeList<MomentRow>()
        momentRows.value = rows
        publishMoments(rows)
        publishComments(
            cloud.supabase.from("moment_comments").select().decodeList<CommentRow>(),
        )
        refreshLikes()
        publishMoments(rows)
    }

    override suspend fun createMoment(
        eventId: String,
        image: ByteArray,
        mimeType: String,
        caption: String,
    ) {
        require(image.isNotEmpty()) { "Choose a photo to upload." }
        require(image.size <= MAX_IMAGE_BYTES) { "Photos must be 6 MB or smaller." }
        require(mimeType in SUPPORTED_IMAGE_TYPES) { "Use a JPEG, PNG, or WebP photo." }
        val user = requireNotNull(authRepository.session.value.user) { "Sign in to add a moment." }
        val momentId = UUID.randomUUID().toString()
        val path = "$eventId/${user.id}/$momentId.${mimeType.extension()}"
        val bucket = cloud.supabase.storage.from(MOMENT_BUCKET)

        bucket.upload(path, image) {
            upsert = false
            contentType = ContentType.parse(mimeType)
        }
        try {
            cloud.supabase.from("event_moments").insert(
                NewMomentRow(
                    id = momentId,
                    eventId = eventId,
                    authorId = user.id,
                    authorName = user.displayName,
                    imagePath = path,
                    caption = caption.trim(),
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
        } catch (error: Throwable) {
            runCatching { bucket.delete(path) }
            throw error
        }
        refresh()
    }

    override suspend fun toggleLike(moment: EventMoment) {
        val userId = requireUserId()
        if (moment.likedByCurrentUser) {
            cloud.supabase.from("moment_likes").delete {
                filter {
                    eq("moment_id", moment.id)
                    eq("user_id", userId)
                }
            }
            likedMomentIds.value = likedMomentIds.value - moment.id
        } else {
            cloud.supabase.from("moment_likes").insert(
                MomentLikeRow(momentId = moment.id, userId = userId),
            )
            likedMomentIds.value = likedMomentIds.value + moment.id
        }
        publishMoments(momentRows.value)
        refresh()
    }

    override suspend fun addComment(momentId: String, body: String) {
        val cleanBody = body.trim()
        require(cleanBody.isNotEmpty()) { "Write a comment first." }
        require(cleanBody.length <= 500) { "Comments can contain up to 500 characters." }
        val user = requireNotNull(authRepository.session.value.user) { "Sign in to comment." }
        cloud.supabase.from("moment_comments").insert(
            NewCommentRow(
                momentId = momentId,
                authorId = user.id,
                authorName = user.displayName,
                body = cleanBody,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
        refresh()
    }

    override suspend fun recordView(momentId: String) {
        val userId = authRepository.session.value.user?.id ?: return
        synchronized(viewedMomentIds) {
            if (!viewedMomentIds.add(momentId)) return
        }
        runCatching {
            cloud.supabase.from("moment_views").insert(
                MomentViewRow(momentId = momentId, userId = userId),
            )
        }
    }

    override suspend fun createShareUrl(moment: EventMoment): String =
        cloud.supabase.storage.from(MOMENT_BUCKET)
            .createSignedUrl(moment.imagePath, 1.hours)

    override suspend fun deleteMoment(moment: EventMoment) {
        require(moment.ownedByCurrentUser || authRepository.session.value.isAdmin) {
            "Only the author or a Poi administrator can delete this moment."
        }
        cloud.supabase.from("event_moments").delete {
            filter { eq("id", moment.id) }
        }
        runCatching { cloud.supabase.storage.from(MOMENT_BUCKET).delete(moment.imagePath) }
        refresh()
    }

    private suspend fun refreshLikes() {
        val userId = authRepository.session.value.user?.id
        likedMomentIds.value = if (userId == null) {
            emptySet()
        } else {
            runCatching {
                cloud.supabase.from("moment_likes").select {
                    filter { eq("user_id", userId) }
                }.decodeList<MomentLikeRow>().mapTo(mutableSetOf(), MomentLikeRow::momentId)
            }.getOrDefault(emptySet())
        }
    }

    private suspend fun publishMoments(rows: List<MomentRow>) {
        val currentUserId = authRepository.session.value.user?.id
        val liked = likedMomentIds.value
        val existingUrls = _moments.value.associate { it.imagePath to it.imageUrl }
        _moments.value = rows.sortedByDescending(MomentRow::createdAtMillis).map { row ->
            val imageUrl = runCatching {
                cloud.supabase.storage.from(MOMENT_BUCKET)
                    .createSignedUrl(row.imagePath, 2.hours)
            }.getOrElse { existingUrls[row.imagePath].orEmpty() }
            row.toModel(
                imageUrl = imageUrl,
                likedByCurrentUser = row.id in liked,
                ownedByCurrentUser = authRepository.session.value.isAdmin ||
                    (currentUserId != null && row.authorId == currentUserId),
            )
        }
    }

    private fun publishComments(rows: List<CommentRow>) {
        val currentUserId = authRepository.session.value.user?.id
        _comments.value = rows.sortedBy(CommentRow::createdAtMillis).map { row ->
            row.toModel(currentUserId != null && row.authorId == currentUserId)
        }
    }

    private fun requireUserId(): String =
        requireNotNull(authRepository.session.value.user?.id) { "Sign in to continue." }

    private companion object {
        const val MOMENT_BUCKET = "event-moments"
        const val MAX_IMAGE_BYTES = 6 * 1024 * 1024
        val SUPPORTED_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
}

@Serializable
private data class MomentRow(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("image_path") val imagePath: String,
    val caption: String = "",
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("created_at_millis") val createdAtMillis: Long,
)

@Serializable
private data class NewMomentRow(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("image_path") val imagePath: String,
    val caption: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
)

@Serializable
private data class MomentLikeRow(
    @SerialName("moment_id") val momentId: String,
    @SerialName("user_id") val userId: String,
)

@Serializable
private data class CommentRow(
    val id: String,
    @SerialName("moment_id") val momentId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String,
    val body: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
)

@Serializable
private data class NewCommentRow(
    @SerialName("moment_id") val momentId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String,
    val body: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
)

@Serializable
private data class MomentViewRow(
    @SerialName("moment_id") val momentId: String,
    @SerialName("user_id") val userId: String,
)

private fun MomentRow.toModel(
    imageUrl: String,
    likedByCurrentUser: Boolean,
    ownedByCurrentUser: Boolean,
): EventMoment = EventMoment(
    id = id,
    eventId = eventId,
    authorId = authorId,
    authorName = authorName,
    imagePath = imagePath,
    imageUrl = imageUrl,
    caption = caption,
    likeCount = likeCount,
    commentCount = commentCount,
    viewCount = viewCount,
    likedByCurrentUser = likedByCurrentUser,
    ownedByCurrentUser = ownedByCurrentUser,
    createdAtMillis = createdAtMillis,
)

private fun CommentRow.toModel(ownedByCurrentUser: Boolean): MomentComment = MomentComment(
    id = id,
    momentId = momentId,
    authorId = authorId,
    authorName = authorName,
    body = body,
    ownedByCurrentUser = ownedByCurrentUser,
    createdAtMillis = createdAtMillis,
)

private fun String.extension(): String = when (this) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    else -> "jpg"
}
