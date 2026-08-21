package com.poi.core.cloud

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime

data class PoiCloudConfig(
    val projectUrl: String,
    val publishableKey: String,
) {
    val isConfigured: Boolean
        get() = projectUrl.startsWith("https://") &&
            projectUrl.endsWith(".supabase.co") &&
            publishableKey.isNotBlank()
}

class PoiCloudClient private constructor(
    val supabase: SupabaseClient,
) {
    fun handleAuthCallback(intent: Intent) {
        supabase.handleDeeplinks(intent)
    }

    companion object {
        fun createOrNull(config: PoiCloudConfig): PoiCloudClient? {
            if (!config.isConfigured) return null
            return PoiCloudClient(
                createSupabaseClient(
                    supabaseUrl = config.projectUrl,
                    supabaseKey = config.publishableKey,
                ) {
                    install(Auth) {
                        scheme = "poi"
                        host = "auth-callback"
                    }
                    install(Postgrest)
                    install(Realtime)
                },
            )
        }
    }
}
