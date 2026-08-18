package com.poi.core.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminPolicyTest {
    @Test
    fun policyRequiresEmailAndSha256Hash() {
        assertFalse(AdminPolicy("", "a".repeat(64)).isConfigured)
        assertFalse(AdminPolicy("owner@example.com", "short").isConfigured)
        assertTrue(AdminPolicy("owner@example.com", "a".repeat(64)).isConfigured)
    }
}
