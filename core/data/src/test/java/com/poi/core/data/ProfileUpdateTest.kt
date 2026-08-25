package com.poi.core.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ProfileUpdateTest {
    @Test
    fun normalizesSafeProfileValues() {
        val update = normalizeProfileUpdate(
            displayName = "  Dhanush Shriyan  ",
            handle = " @Dhanush_08 ",
            homeArea = "  Mangaluru  ",
        )

        assertEquals("Dhanush Shriyan", update.displayName)
        assertEquals("@dhanush_08", update.handle)
        assertEquals("Mangaluru", update.homeArea)
    }

    @Test
    fun rejectsUnsafeOrIncompleteProfileValues() {
        assertThrows(IllegalArgumentException::class.java) {
            normalizeProfileUpdate("", "valid_name", "Mangaluru")
        }
        assertThrows(IllegalArgumentException::class.java) {
            normalizeProfileUpdate("Dhanush", "not-valid!", "Mangaluru")
        }
        assertThrows(IllegalArgumentException::class.java) {
            normalizeProfileUpdate("Dhanush", "valid_name", "")
        }
    }
}
