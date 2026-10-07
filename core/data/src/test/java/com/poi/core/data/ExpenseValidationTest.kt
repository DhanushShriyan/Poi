package com.poi.core.data

import com.poi.core.model.ExpenseAllocation
import com.poi.core.model.ExpenseCategory
import com.poi.core.model.ExpenseSplitMethod
import com.poi.core.model.NewEventExpense
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ExpenseValidationTest {
    @Test
    fun `valid expense and upi id pass normalization`() {
        validateExpense(expense())
        assertEquals("dhanush@upi", normalizeUpiId(" Dhanush@UPI "))
        assertEquals(null, normalizeUpiId("  "))
    }

    @Test
    fun `allocations must equal total`() {
        assertThrows(IllegalArgumentException::class.java) {
            validateExpense(expense().copy(shares = listOf(ExpenseAllocation("u", 999))))
        }
    }

    private fun expense() = NewEventExpense(
        groupId = "group",
        title = "Tickets",
        note = "",
        category = ExpenseCategory.TICKETS,
        currency = "INR",
        amountMinor = 1_000,
        splitMethod = ExpenseSplitMethod.EQUAL,
        payments = listOf(ExpenseAllocation("u", 1_000)),
        shares = listOf(ExpenseAllocation("u", 1_000)),
    )
}
