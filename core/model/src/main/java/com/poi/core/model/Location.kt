package com.poi.core.model

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90." }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180." }
    }
}

data class LocationSnapshot(
    val point: GeoPoint,
    val accuracyMeters: Float,
    val capturedAtMillis: Long,
)

enum class AttendanceVerificationMethod(val label: String) {
    MANUAL("Manual check-in"),
    PROXIMITY("Location confirmed"),
}

data class AttendanceVerification(
    val method: AttendanceVerificationMethod,
    val distanceMeters: Int? = null,
    val accuracyMeters: Int? = null,
    val verifiedAtMillis: Long? = null,
)

data class AttendanceEvidence(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
)

fun GeoPoint.distanceMetersTo(other: GeoPoint): Double {
    val earthRadiusMeters = 6_371_000.0
    val latitudeDelta = Math.toRadians(other.latitude - latitude)
    val longitudeDelta = Math.toRadians(other.longitude - longitude)
    val startLatitude = Math.toRadians(latitude)
    val endLatitude = Math.toRadians(other.latitude)
    val haversine = sin(latitudeDelta / 2).let { it * it } +
        cos(startLatitude) * cos(endLatitude) *
        sin(longitudeDelta / 2).let { it * it }
    return earthRadiusMeters * 2 * asin(sqrt(haversine.coerceIn(0.0, 1.0)))
}

fun Event.geoPointOrNull(): GeoPoint? {
    val eventLatitude = latitude ?: return null
    val eventLongitude = longitude ?: return null
    return runCatching { GeoPoint(eventLatitude, eventLongitude) }.getOrNull()
}

fun Event.distanceMetersFrom(location: LocationSnapshot): Double? =
    geoPointOrNull()?.distanceMetersTo(location.point)

fun Event.withDistanceFrom(location: LocationSnapshot): Event {
    val calculatedMeters = distanceMetersFrom(location) ?: return this
    return copy(distanceKm = calculatedMeters / 1_000.0)
}

fun Event.isWithinCheckInRange(location: LocationSnapshot): Boolean {
    val distance = distanceMetersFrom(location) ?: return false
    val accuracyAllowance = location.accuracyMeters.coerceIn(0f, 200f)
    return distance <= checkInRadiusMeters + accuracyAllowance
}

fun AttendanceEvidence.toVerification(distanceMeters: Double): AttendanceVerification =
    AttendanceVerification(
        method = AttendanceVerificationMethod.PROXIMITY,
        distanceMeters = distanceMeters.roundToInt(),
        accuracyMeters = accuracyMeters.roundToInt(),
        verifiedAtMillis = System.currentTimeMillis(),
    )
