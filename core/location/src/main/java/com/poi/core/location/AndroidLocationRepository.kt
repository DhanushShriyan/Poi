package com.poi.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.poi.core.model.GeoPoint
import com.poi.core.model.LocationSnapshot
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout

class AndroidLocationRepository(context: Context) : LocationRepository {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val _state = MutableStateFlow(PoiLocationState())
    override val state: StateFlow<PoiLocationState> = _state.asStateFlow()

    override suspend fun refresh(): Result<LocationSnapshot> {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        return runCatching {
            check(hasLocationPermission()) { "Allow location access to use nearby discovery." }
            check(LocationManagerCompat.isLocationEnabled(locationManager)) {
                "Turn on device location, then retry."
            }
            val location = withTimeout(15_000) { currentLocation() }
            checkNotNull(location) { "Poi could not get a current location. Move outdoors and retry." }
            LocationSnapshot(
                point = GeoPoint(location.latitude, location.longitude),
                accuracyMeters = location.accuracy.coerceAtLeast(0f),
                capturedAtMillis = location.time.takeIf { it > 0 } ?: System.currentTimeMillis(),
            )
        }.onSuccess { snapshot ->
            _state.value = PoiLocationState(snapshot = snapshot)
        }.onFailure { error ->
            _state.value = _state.value.copy(
                isLoading = false,
                errorMessage = error.message ?: "Location is temporarily unavailable.",
            )
        }
    }

    override fun clear() {
        _state.value = PoiLocationState()
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? {
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter(locationManager::isProviderEnabled)
        val provider = providers.firstOrNull() ?: return null
        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(appContext),
            ) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        }
    }
}
