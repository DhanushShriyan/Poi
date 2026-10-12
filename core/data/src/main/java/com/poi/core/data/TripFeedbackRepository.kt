package com.poi.core.data

import com.poi.core.auth.AuthRepository
import com.poi.core.cloud.PoiCloudClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TripFeedback(
    val id: String = "",
    val category: String,
    val rating: Int,
    val message: String,
    val platform: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("created_at") val createdAt: String = "",
)

class TripFeedbackRepository(private val cloud: PoiCloudClient?, private val auth: AuthRepository) {
    suspend fun submit(category: String, rating: Int, message: String, appVersion: String) {
        val user = requireNotNull(auth.session.value.user) { "Sign in to send feedback." }
        validateTripFeedback(category, rating, message)
        val client = requireNotNull(cloud) { "Feedback needs the live cloud connection." }
        client.supabase.from("tester_feedback").insert(NewTripFeedback(user.id, category, rating, message.trim(), appVersion = appVersion))
    }
    suspend fun history(): List<TripFeedback> {
        val user = requireNotNull(auth.session.value.user) { "Sign in to view your feedback." }
        return requireNotNull(cloud).supabase.from("tester_feedback").select {
            filter { eq("user_id", user.id) }
        }.decodeList<TripFeedback>().sortedByDescending { it.createdAt }
    }
}

internal fun validateTripFeedback(category: String, rating: Int, message: String) {
    require(category in listOf("login", "photos", "expenses", "events", "other")) { "Choose a feedback category." }
    require(rating in 1..5) { "Choose a rating from 1 to 5." }
    require(message.trim().length in 10..2000) { "Describe your feedback in 10–2000 characters." }
}

@Serializable private data class NewTripFeedback(
    @SerialName("user_id") val userId: String,
    val category: String, val rating: Int, val message: String,
    val platform: String = "android",
    @SerialName("app_version") val appVersion: String,
)
