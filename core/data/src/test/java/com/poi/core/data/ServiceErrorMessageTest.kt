package com.poi.core.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceErrorMessageTest {
    @Test
    fun `network failures use a safe retry message`() {
        assertEquals(
            "Poi could not reach the service. Check your connection and retry.",
            serviceErrorMessage(IllegalStateException("No address associated with hostname")),
        )
    }

    @Test
    fun `long backend errors are not shown in full`() {
        val message = "x".repeat(240)

        assertEquals(180, serviceErrorMessage(IllegalStateException(message)).length)
    }
}
