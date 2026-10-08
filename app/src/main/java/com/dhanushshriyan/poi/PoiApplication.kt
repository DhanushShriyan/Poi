package com.dhanushshriyan.poi

import android.app.Application
import com.poi.core.auth.AdminPolicy
import com.poi.core.auth.AuthRepository
import com.poi.core.auth.LocalAuthRepository
import com.poi.core.auth.SupabaseAuthRepository
import com.poi.core.cloud.PoiCloudClient
import com.poi.core.cloud.PoiCloudConfig
import com.poi.core.data.EventRepository
import com.poi.core.data.ExpenseRepository
import com.poi.core.data.LocalEventRepository
import com.poi.core.data.LocalExpenseRepository
import com.poi.core.data.LocalMomentRepository
import com.poi.core.data.LocalSocialRepository
import com.poi.core.data.MomentRepository
import com.poi.core.data.SocialRepository
import com.poi.core.data.SupabaseEventRepository
import com.poi.core.data.SupabaseExpenseRepository
import com.poi.core.data.SupabaseMomentRepository
import com.poi.core.data.SupabaseSocialRepository
import com.poi.core.location.AndroidLocationRepository
import com.poi.core.location.LocationRepository
import com.poi.core.notifications.EventReminderScheduler
import com.poi.core.poster.CloudPosterReader
import io.github.jan.supabase.auth.auth

class PoiApplication : Application() {
    val cloudPosterReader: CloudPosterReader? by lazy {
        cloudClient?.let { cloud ->
            CloudPosterReader(applicationContext, BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
                cloud.supabase.auth.currentSessionOrNull()?.accessToken
            }
        }
    }
    private val cloudClient: PoiCloudClient? by lazy {
        PoiCloudClient.createOrNull(
            PoiCloudConfig(
                projectUrl = BuildConfig.SUPABASE_URL,
                publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
            ),
        )
    }

    val authRepository: AuthRepository by lazy {
        cloudClient?.let { cloud ->
            SupabaseAuthRepository(
                cloud = cloud,
                supportsGoogleSignIn = BuildConfig.GOOGLE_AUTH_ENABLED,
                supportsPhoneSignIn = BuildConfig.PHONE_AUTH_ENABLED,
            )
        } ?: LocalAuthRepository(
                applicationContext,
                AdminPolicy(BuildConfig.ADMIN_EMAIL, BuildConfig.ADMIN_CODE_SHA256),
            )
    }

    val eventRepository: EventRepository by lazy {
        cloudClient?.let { cloud ->
            SupabaseEventRepository(applicationContext, cloud, authRepository)
        } ?: LocalEventRepository(applicationContext)
    }

    val momentRepository: MomentRepository by lazy {
        cloudClient?.let { cloud ->
            SupabaseMomentRepository(cloud, authRepository)
        } ?: LocalMomentRepository()
    }

    val expenseRepository: ExpenseRepository by lazy {
        cloudClient?.let { cloud ->
            SupabaseExpenseRepository(cloud, authRepository)
        } ?: LocalExpenseRepository(authRepository)
    }

    val socialRepository: SocialRepository by lazy {
        cloudClient?.let { cloud ->
            SupabaseSocialRepository(cloud, authRepository)
        } ?: LocalSocialRepository(authRepository)
    }

    val locationRepository: LocationRepository by lazy {
        AndroidLocationRepository(applicationContext)
    }

    val reminderScheduler: EventReminderScheduler by lazy {
        EventReminderScheduler(applicationContext)
    }
}
