package com.poi.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpenseTest {
    private val ananya = member("a", "Ananya")
    private val rohan = member("r", "Rohan")
    private val meera = member("m", "Meera")

    @Test
    fun `equal allocation preserves every paise`() {
        assertEquals(
            listOf(334L, 333L, 333L),
            allocateEvenly(1_000, listOf("a", "r", "m")).map(ExpenseAllocation::amountMinor),
        )
    }

    @Test
    fun `balances include confirmed settlements and simplify debts`() {
        val expense = EventExpense(
            id = "expense", groupId = "group", createdBy = "a", createdByName = "Ananya",
            title = "Tickets", note = "", category = ExpenseCategory.TICKETS,
            currency = "INR", amountMinor = 3_000, splitMethod = ExpenseSplitMethod.EQUAL,
            payments = listOf(ExpenseAllocation("a", 3_000)),
            shares = listOf(
                ExpenseAllocation("a", 1_000), ExpenseAllocation("r", 1_000),
                ExpenseAllocation("m", 1_000),
            ),
            createdAtMillis = 1, updatedAtMillis = 1,
        )
        val confirmed = ExpenseSettlement(
            id = "settlement", groupId = "group", payerId = "r", payeeId = "a",
            payerName = "Rohan", payeeName = "Ananya", amountMinor = 500,
            currency = "INR", note = "", status = ExpenseSettlementStatus.CONFIRMED,
            createdAtMillis = 2,
        )

        val balances = calculateExpenseBalances(listOf(ananya, rohan, meera), listOf(expense), listOf(confirmed))
        assertEquals(mapOf("a" to 1_500L, "r" to -500L, "m" to -1_000L), balances.associate { it.member.userId to it.amountMinor })
        assertEquals(
            listOf("m" to 1_000L, "r" to 500L),
            simplifyExpenseBalances(balances).map { it.payer.userId to it.amountMinor },
        )
    }

    private fun member(id: String, name: String) = ExpenseMember(
        groupId = "group", userId = id, displayName = name, status = ExpenseMemberStatus.ACTIVE,
    )
}
