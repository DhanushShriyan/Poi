package com.poi.feature.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInputTest {
    @Test
    fun `new account requires name valid email and strong enough password`() {
        assertEquals(
            "Enter your name.",
            validateEmailAccountInput("", "person@example.com", "Password9", true),
        )
        assertEquals(
            "Enter a valid email address.",
            validateEmailAccountInput("Person", "not-an-email", "Password9", true),
        )
        assertEquals(
            "Use at least 8 characters with letters and numbers.",
            validateEmailAccountInput("Person", "person@example.com", "password", true),
        )
        assertNull(
            validateEmailAccountInput("Person", "person@example.com", "Password9", true),
        )
    }

    @Test
    fun `network and credential failures are human readable`() {
        assertEquals(
            "Poi cannot reach the account service. Check your internet connection and try again.",
            friendlyAuthError(IllegalStateException("Unable to resolve host backend.example")),
        )
        assertEquals(
            "The email or password is incorrect.",
            friendlyAuthError(IllegalArgumentException("Invalid login credentials")),
        )
    }
}
