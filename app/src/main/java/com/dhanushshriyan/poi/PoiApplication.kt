package com.dhanushshriyan.poi

import android.app.Application
import com.poi.core.data.EventRepository
import com.poi.core.data.LocalEventRepository

class PoiApplication : Application() {
    val eventRepository: EventRepository by lazy { LocalEventRepository(applicationContext) }
}

