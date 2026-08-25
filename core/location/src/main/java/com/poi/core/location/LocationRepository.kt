package com.poi.core.location

import com.poi.core.model.LocationSnapshot
import kotlinx.coroutines.flow.StateFlow

data class PoiLocationState(
    val isLoading: Boolean = false,
    val snapshot: LocationSnapshot? = null,
    val errorMessage: String? = null,
)

interface LocationRepository {
    val state: StateFlow<PoiLocationState>

    suspend fun refresh(): Result<LocationSnapshot>
    fun clear()
}
