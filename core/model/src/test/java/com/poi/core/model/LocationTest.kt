package com.poi.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationTest {
    @Test
    fun distanceUsesHaversineInsteadOfStoredListingDistance() {
        val kadri = GeoPoint(12.8867, 74.8556)
        val nearby = GeoPoint(12.8957, 74.8556)

        val distance = kadri.distanceMetersTo(nearby)

        assertTrue(distance in 995.0..1_010.0)
    }

    @Test
    fun checkInRangeIncludesCappedAccuracyAllowance() {
        val event = testEvent(latitude = 12.8867, longitude = 74.8556, radius = 500)
        val closeLocation = LocationSnapshot(GeoPoint(12.8917, 74.8556), 80f, 1L)
        val farLocation = LocationSnapshot(GeoPoint(12.8967, 74.8556), 500f, 1L)

        assertTrue(event.isWithinCheckInRange(closeLocation))
        assertFalse(event.isWithinCheckInRange(farLocation))
    }

    @Test
    fun missingVenueCoordinatesKeepExistingDisplayDistance() {
        val event = testEvent(latitude = null, longitude = null, radius = 500).copy(distanceKm = 7.5)
        val location = LocationSnapshot(GeoPoint(12.8867, 74.8556), 10f, 1L)

        assertEquals(7.5, event.withDistanceFrom(location).distanceKm, 0.0)
    }

    private fun testEvent(latitude: Double?, longitude: Double?, radius: Int) = Event(
        id = "event",
        title = "Event",
        summary = "Summary",
        description = "Description",
        category = EventCategory.COMMUNITY,
        startsAtMillis = 1L,
        endsAtMillis = 2L,
        venue = "Venue",
        address = "Address",
        distanceKm = 0.0,
        organizer = Organizer("Organizer", false),
        visibility = EventVisibility.PUBLIC,
        verification = VerificationLevel.COMMUNITY,
        attendeeCount = 0,
        friendNames = emptyList(),
        themeKey = "community",
        latitude = latitude,
        longitude = longitude,
        checkInRadiusMeters = radius,
    )
}
