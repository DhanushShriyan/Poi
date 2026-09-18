package com.poi.feature.create

import org.junit.Assert.assertEquals
import org.junit.Test

class CreateEventErrorTest {
    @Test
    fun `network failures explain how to retry`() {
        assertEquals(
            "Poi could not reach the event service. Check your internet connection and try again.",
            createEventErrorMessage(IllegalStateException("Unable to resolve host backend.example")),
        )
    }

    @Test
    fun `unknown failures do not expose internal details`() {
        assertEquals(
            "We couldn't publish this event. Please review the details and try again.",
            createEventErrorMessage(IllegalStateException("internal database detail")),
        )
    }
}
