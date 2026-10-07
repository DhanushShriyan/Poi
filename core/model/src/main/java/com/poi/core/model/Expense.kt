package com.poi.core.model

enum class ExpenseCategory(val label: String, val symbol: String) {
    TICKETS("Tickets", "🎟"),
    TRAVEL("Travel", "↗"),
    FOOD("Food", "◌"),
    STAY("Stay", "⌂"),
    SHOPPING("Shopping", "◇"),
    OTHER("Other", "+")
}

enum class ExpenseSplitMethod(val label: String) {
    EQUAL("Equally"),
    EXACT("Exact amounts"),
    PERCENTAGE("Percentages"),
    SHARES("Shares")
}

enum class ExpenseMemberStatus {
    INVITED,
    ACTIVE,
    DECLINED
}

enum class ExpenseSettlementStatus {
    PENDING,
    CONFIRMED
}

enum class ExpenseActivityAction(val label: String) {
    EXPENSE_ADDED("added an expense"),
    EXPENSE_UPDATED("updated an expense"),
    EXPENSE_DELETED("deleted an expense"),
    SETTLEMENT_RECORDED("recorded a payment"),
    SETTLEMENT_CONFIRMED("confirmed a payment")
}

data class EventExpenseGroup(
    val id: String,
    val eventId: String,
    val createdBy: String,
    val currency: String,
    val createdAtMillis: Long,
)

data class ExpenseMember(
    val groupId: String,
    val userId: String,
    val displayName: String,
    val status: ExpenseMemberStatus,
    val upiId: String? = null,
    val invitedBy: String? = null,
)

data class ExpenseAllocation(
    val userId: String,
    val amountMinor: Long,
)

data class EventExpense(
    val id: String,
    val groupId: String,
    val createdBy: String?,
    val createdByName: String,
    val title: String,
    val note: String,
    val category: ExpenseCategory,
    val currency: String,
    val amountMinor: Long,
    val splitMethod: ExpenseSplitMethod,
    val receiptPath: String? = null,
    val receiptUrl: String? = null,
    val payments: List<ExpenseAllocation>,
    val shares: List<ExpenseAllocation>,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val deletedAtMillis: Long? = null,
) {
    val isDeleted: Boolean get() = deletedAtMillis != null
}

data class ExpenseComment(
    val id: String,
    val expenseId: String,
    val authorId: String,
    val authorName: String,
    val body: String,
    val createdAtMillis: Long,
)

data class ExpenseSettlement(
    val id: String,
    val groupId: String,
    val payerId: String,
    val payeeId: String,
    val payerName: String,
    val payeeName: String,
    val amountMinor: Long,
    val currency: String,
    val note: String,
    val status: ExpenseSettlementStatus,
    val createdAtMillis: Long,
    val confirmedAtMillis: Long? = null,
)

data class ExpenseActivity(
    val id: String,
    val groupId: String,
    val actorId: String?,
    val actorName: String,
    val action: ExpenseActivityAction,
    val subjectId: String,
    val summary: String,
    val createdAtMillis: Long,
)

data class NewEventExpense(
    val groupId: String,
    val title: String,
    val note: String,
    val category: ExpenseCategory,
    val currency: String,
    val amountMinor: Long,
    val splitMethod: ExpenseSplitMethod,
    val payments: List<ExpenseAllocation>,
    val shares: List<ExpenseAllocation>,
)

data class ExpenseBalance(
    val member: ExpenseMember,
    val amountMinor: Long,
)

data class SuggestedSettlement(
    val payer: ExpenseMember,
    val payee: ExpenseMember,
    val amountMinor: Long,
)

data class ExpenseSyncState(
    val isCloudBacked: Boolean,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastSyncedAtMillis: Long? = null,
)

fun allocateEvenly(totalMinor: Long, userIds: List<String>): List<ExpenseAllocation> {
    require(totalMinor > 0) { "Total must be positive." }
    require(userIds.isNotEmpty()) { "Choose at least one person." }
    val distinctIds = userIds.distinct()
    val base = totalMinor / distinctIds.size
    var remainder = totalMinor % distinctIds.size
    return distinctIds.map { userId ->
        val amount = base + if (remainder > 0) 1 else 0
        if (remainder > 0) remainder--
        ExpenseAllocation(userId, amount)
    }
}

fun calculateExpenseBalances(
    members: List<ExpenseMember>,
    expenses: List<EventExpense>,
    settlements: List<ExpenseSettlement>,
): List<ExpenseBalance> {
    val activeMembers = members.filter { it.status == ExpenseMemberStatus.ACTIVE }
    val balances = activeMembers.associate { it.userId to 0L }.toMutableMap()
    expenses.filterNot(EventExpense::isDeleted).forEach { expense ->
        expense.payments.forEach { payment ->
            balances.computeIfPresent(payment.userId) { _, current -> current + payment.amountMinor }
        }
        expense.shares.forEach { share ->
            balances.computeIfPresent(share.userId) { _, current -> current - share.amountMinor }
        }
    }
    settlements.filter { it.status == ExpenseSettlementStatus.CONFIRMED }.forEach { settlement ->
        balances.computeIfPresent(settlement.payerId) { _, current -> current + settlement.amountMinor }
        balances.computeIfPresent(settlement.payeeId) { _, current -> current - settlement.amountMinor }
    }
    return activeMembers.map { ExpenseBalance(it, balances.getValue(it.userId)) }
}

fun simplifyExpenseBalances(balances: List<ExpenseBalance>): List<SuggestedSettlement> {
    data class MutableBalance(val member: ExpenseMember, var amount: Long)
    val debtors = balances.filter { it.amountMinor < 0 }
        .map { MutableBalance(it.member, -it.amountMinor) }
        .sortedByDescending(MutableBalance::amount)
        .toMutableList()
    val creditors = balances.filter { it.amountMinor > 0 }
        .map { MutableBalance(it.member, it.amountMinor) }
        .sortedByDescending(MutableBalance::amount)
        .toMutableList()
    val suggestions = mutableListOf<SuggestedSettlement>()
    var debtorIndex = 0
    var creditorIndex = 0
    while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
        val debtor = debtors[debtorIndex]
        val creditor = creditors[creditorIndex]
        val amount = minOf(debtor.amount, creditor.amount)
        if (amount > 0) suggestions += SuggestedSettlement(debtor.member, creditor.member, amount)
        debtor.amount -= amount
        creditor.amount -= amount
        if (debtor.amount == 0L) debtorIndex++
        if (creditor.amount == 0L) creditorIndex++
    }
    return suggestions
}
