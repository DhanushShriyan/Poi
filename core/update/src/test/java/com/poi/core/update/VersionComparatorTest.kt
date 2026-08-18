package com.poi.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {
    @Test
    fun newerPatchVersionIsDetected() {
        assertTrue(VersionComparator.isNewer("v0.1.2", "0.1.1"))
    }

    @Test
    fun equalAndOlderVersionsAreIgnored() {
        assertFalse(VersionComparator.isNewer("0.1.1", "0.1.1"))
        assertFalse(VersionComparator.isNewer("0.1.0", "0.1.1"))
    }

    @Test
    fun testSuffixDoesNotBreakComparison() {
        assertTrue(VersionComparator.isNewer("0.2.0", "0.1.9-test"))
    }
}
