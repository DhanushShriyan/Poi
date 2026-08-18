package com.dhanushshriyan.poi

import android.app.Application
import com.poi.core.auth.AdminPolicy
import com.poi.core.auth.AuthRepository
import com.poi.core.auth.LocalAuthRepository
import com.poi.core.data.EventRepository
import com.poi.core.data.LocalEventRepository

class PoiApplication : Application() {
    val eventRepository: EventRepository by lazy { LocalEventRepository(applicationContext) }
    val authRepository: AuthRepository by lazy {
        LocalAuthRepository(
            applicationContext,
            AdminPolicy(BuildConfig.ADMIN_EMAIL, BuildConfig.ADMIN_CODE_SHA256),
        )
    }
}
