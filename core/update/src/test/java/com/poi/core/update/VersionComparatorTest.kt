package com.poi.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

class DownloadProgressTest {
    @Test
    fun fractionIsUnavailableWhenTotalSizeIsUnknown() {
        assertNull(DownloadProgress(bytesDownloaded = 512L, totalBytes = null).fraction)
    }

    @Test
    fun fractionReportsAndClampsDownloadProgress() {
        assertEquals(0.5f, DownloadProgress(500L, 1_000L).fraction)
        assertEquals(1f, DownloadProgress(1_500L, 1_000L).fraction)
    }
}
