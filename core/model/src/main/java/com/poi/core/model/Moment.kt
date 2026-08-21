package com.poi.core.model

data class EventMoment(
    val id: String,
    val eventId: String,
    val authorId: String,
    val authorName: String,
    val imagePath: String,
    val imageUrl: String,
    val caption: String,
    val likeCount: Int,
    val commentCount: Int,
    val viewCount: Int,
    val likedByCurrentUser: Boolean,
    val ownedByCurrentUser: Boolean,
    val createdAtMillis: Long,
)

data class MomentComment(
    val id: String,
    val momentId: String,
    val authorId: String,
    val authorName: String,
    val body: String,
    val ownedByCurrentUser: Boolean,
    val createdAtMillis: Long,
)
