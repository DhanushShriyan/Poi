package com.poi.core.data

import com.poi.core.model.EventExpense
import com.poi.core.model.EventExpenseGroup
import com.poi.core.model.ExpenseActivity
import com.poi.core.model.ExpenseComment
import com.poi.core.model.ExpenseMember
import com.poi.core.model.ExpenseSettlement
import com.poi.core.model.ExpenseSyncState
import com.poi.core.model.NewEventExpense
import kotlinx.coroutines.flow.StateFlow

interface ExpenseRepository {
    val groups: StateFlow<List<EventExpenseGroup>>
    val members: StateFlow<List<ExpenseMember>>
    val expenses: StateFlow<List<EventExpense>>
    val comments: StateFlow<List<ExpenseComment>>
    val settlements: StateFlow<List<ExpenseSettlement>>
    val activity: StateFlow<List<ExpenseActivity>>
    val syncState: StateFlow<ExpenseSyncState>

    suspend fun refresh()
    suspend fun startGroup(eventId: String, currency: String = "INR"): EventExpenseGroup
    suspend fun inviteMember(groupId: String, profileId: String)
    suspend fun respondToInvitation(groupId: String, accept: Boolean)
    suspend fun updateUpiId(groupId: String, upiId: String?)
    suspend fun createExpense(
        expense: NewEventExpense,
        receipt: ByteArray? = null,
        receiptMimeType: String? = null,
    ): EventExpense
    suspend fun updateExpense(expenseId: String, expense: NewEventExpense)
    suspend fun deleteExpense(expenseId: String)
    suspend fun addComment(expenseId: String, body: String)
    suspend fun deleteComment(commentId: String)
    suspend fun recordSettlement(
        groupId: String,
        payeeId: String,
        amountMinor: Long,
        currency: String,
        note: String,
    )
    suspend fun confirmSettlement(settlementId: String)
}

internal fun validateExpense(expense: NewEventExpense) {
    require(expense.title.trim().isNotEmpty()) { "Give this expense a name." }
    require(expense.title.trim().length <= 100) { "Expense names can contain up to 100 characters." }
    require(expense.note.length <= 500) { "Notes can contain up to 500 characters." }
    require(expense.currency.matches(Regex("^[A-Z]{3}$"))) { "Choose a valid currency." }
    require(expense.amountMinor in 1..100_000_000_000L) { "Enter a valid amount." }
    require(expense.payments.isNotEmpty()) { "Choose at least one payer." }
    require(expense.shares.isNotEmpty()) { "Choose at least one participant." }
    require(expense.payments.map { it.userId }.distinct().size == expense.payments.size) {
        "Each payer can appear only once."
    }
    require(expense.shares.map { it.userId }.distinct().size == expense.shares.size) {
        "Each participant can appear only once."
    }
    require(expense.payments.all { it.amountMinor > 0 }) { "Payer amounts must be positive." }
    require(expense.shares.all { it.amountMinor >= 0 }) { "Split amounts cannot be negative." }
    require(expense.payments.sumOf { it.amountMinor } == expense.amountMinor) {
        "Payer amounts must equal the expense total."
    }
    require(expense.shares.sumOf { it.amountMinor } == expense.amountMinor) {
        "Everyone's shares must equal the expense total."
    }
}

internal fun normalizeUpiId(value: String?): String? {
    val normalized = value?.trim()?.lowercase()?.takeIf(String::isNotEmpty) ?: return null
    require(normalized.matches(Regex("^[a-z0-9._-]{2,64}@[a-z0-9._-]{2,64}$"))) {
        "Enter a valid UPI ID, for example name@bank."
    }
    return normalized
}
