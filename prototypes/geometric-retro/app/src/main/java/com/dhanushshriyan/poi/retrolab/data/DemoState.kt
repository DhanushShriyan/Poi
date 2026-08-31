package com.dhanushshriyan.poi.retrolab.data

import android.content.Context
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit
import org.json.JSONArray
import java.time.LocalDate

/** Preferences are confined to this demo's package; no Poi account or service is contacted. */
class DemoState(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("retro_demo_only", Context.MODE_PRIVATE)
    val today: LocalDate = LocalDate.now()
    val events = demoCatalog(today)
    val rsvps = mutableStateMapOf<String, Rsvp>()
    val likes = mutableStateMapOf<String, Boolean>()
    val comments = mutableStateMapOf<String, List<String>>()

    init {
        events.forEach { event ->
            rsvps[event.id] = runCatching {
                Rsvp.valueOf(preferences.getString("rsvp_" + event.id, "NONE") ?: "NONE")
            }.getOrDefault(Rsvp.NONE)
        }
        demoMoments.forEach { moment ->
            likes[moment.id] = preferences.getBoolean("like_" + moment.id, false)
            comments[moment.id] = runCatching {
                val array = JSONArray(preferences.getString("comments_" + moment.id, "[]"))
                List(array.length()) { array.getString(it) }
            }.getOrDefault(emptyList())
        }
    }

    fun respond(eventId: String, selected: Rsvp) {
        val value = toggleRsvp(rsvps[eventId] ?: Rsvp.NONE, selected)
        rsvps[eventId] = value
        preferences.edit { putString("rsvp_" + eventId, value.name) }
    }

    fun like(momentId: String) {
        val value = likes[momentId] != true
        likes[momentId] = value
        preferences.edit { putBoolean("like_" + momentId, value) }
    }

    fun addComment(momentId: String, text: String): Boolean {
        val value = normalizeComment(text) ?: return false
        val updated = comments[momentId].orEmpty() + value
        comments[momentId] = updated
        preferences.edit { putString("comments_" + momentId, JSONArray(updated).toString()) }
        return true
    }
}
