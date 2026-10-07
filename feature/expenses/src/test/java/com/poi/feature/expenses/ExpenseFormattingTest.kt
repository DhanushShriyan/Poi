package com.poi.feature.expenses

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpenseFormattingTest {
    @Test
    fun `money parser accepts decimal rupees`() {
        assertEquals(12_345L, parseMoneyToMinor("123.45"))
        assertNull(parseMoneyToMinor("zero"))
    }

    @Test
    fun `weighted allocation preserves total`() {
        val result = allocateByWeights(
            1_000,
            linkedMapOf("a" to BigDecimal("50"), "b" to BigDecimal("30"), "c" to BigDecimal("20")),
        )
        assertEquals(listOf(500L, 300L, 200L), result.map { it.amountMinor })
    }
}
