package com.poi.core.data

import com.poi.core.auth.AuthRepository
import com.poi.core.model.EventExpense
import com.poi.core.model.EventExpenseGroup
import com.poi.core.model.ExpenseActivity
import com.poi.core.model.ExpenseActivityAction
import com.poi.core.model.ExpenseComment
import com.poi.core.model.ExpenseMember
import com.poi.core.model.ExpenseMemberStatus
import com.poi.core.model.ExpenseSettlement
import com.poi.core.model.ExpenseSettlementStatus
import com.poi.core.model.ExpenseSyncState
import com.poi.core.model.NewEventExpense
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LocalExpenseRepository(
    private val authRepository: AuthRepository,
) : ExpenseRepository {
    private val _groups = MutableStateFlow<List<EventExpenseGroup>>(emptyList())
    override val groups: StateFlow<List<EventExpenseGroup>> = _groups.asStateFlow()
    private val _members = MutableStateFlow<List<ExpenseMember>>(emptyList())
    override val members: StateFlow<List<ExpenseMember>> = _members.asStateFlow()
    private val _expenses = MutableStateFlow<List<EventExpense>>(emptyList())
    override val expenses: StateFlow<List<EventExpense>> = _expenses.asStateFlow()
    private val _comments = MutableStateFlow<List<ExpenseComment>>(emptyList())
    override val comments: StateFlow<List<ExpenseComment>> = _comments.asStateFlow()
    private val _settlements = MutableStateFlow<List<ExpenseSettlement>>(emptyList())
    override val settlements: StateFlow<List<ExpenseSettlement>> = _settlements.asStateFlow()
    private val _activity = MutableStateFlow<List<ExpenseActivity>>(emptyList())
    override val activity: StateFlow<List<ExpenseActivity>> = _activity.asStateFlow()
    private val _syncState = MutableStateFlow(ExpenseSyncState(isCloudBacked = false))
    override val syncState: StateFlow<ExpenseSyncState> = _syncState.asStateFlow()

    override suspend fun refresh() = markSynced()

    override suspend fun startGroup(eventId: String, currency: String): EventExpenseGroup {
        _groups.value.firstOrNull { it.eventId == eventId }?.let { return it }
        val user = requireUser()
        val now = System.currentTimeMillis()
        val group = EventExpenseGroup(UUID.randomUUID().toString(), eventId, user.id, currency, now)
        _groups.value += group
        _members.value += ExpenseMember(group.id, user.id, user.displayName, ExpenseMemberStatus.ACTIVE)
        markSynced()
        return group
    }

    override suspend fun inviteMember(groupId: String, profileId: String) {
        if (_members.value.any { it.groupId == groupId && it.userId == profileId }) return
        _members.value += ExpenseMember(groupId, profileId, "Invited friend", ExpenseMemberStatus.INVITED)
        markSynced()
    }

    override suspend fun respondToInvitation(groupId: String, accept: Boolean) {
        val user = requireUser()
        _members.value = _members.value.map { member ->
            if (member.groupId == groupId && member.userId == user.id) {
                member.copy(status = if (accept) ExpenseMemberStatus.ACTIVE else ExpenseMemberStatus.DECLINED)
            } else member
        }
        markSynced()
    }

    override suspend fun updateUpiId(groupId: String, upiId: String?) {
        val user = requireUser()
        val normalized = normalizeUpiId(upiId)
        _members.value = _members.value.map { member ->
            if (member.groupId == groupId && member.userId == user.id) member.copy(upiId = normalized)
            else member
        }
        markSynced()
    }

    override suspend fun createExpense(
        expense: NewEventExpense,
        receipt: ByteArray?,
        receiptMimeType: String?,
    ): EventExpense {
        validateExpense(expense)
        val user = requireUser()
        val now = System.currentTimeMillis()
        val created = EventExpense(
            id = UUID.randomUUID().toString(), groupId = expense.groupId,
            createdBy = user.id, createdByName = user.displayName, title = expense.title.trim(),
            note = expense.note.trim(), category = expense.category, currency = expense.currency,
            amountMinor = expense.amountMinor, splitMethod = expense.splitMethod,
            payments = expense.payments, shares = expense.shares,
            createdAtMillis = now, updatedAtMillis = now,
        )
        _expenses.value += created
        addActivity(created.groupId, created.id, ExpenseActivityAction.EXPENSE_ADDED, created.title)
        markSynced()
        return created
    }

    override suspend fun updateExpense(expenseId: String, expense: NewEventExpense) {
        validateExpense(expense)
        _expenses.value = _expenses.value.map { current ->
            if (current.id == expenseId) current.copy(
                title = expense.title.trim(), note = expense.note.trim(), category = expense.category,
                amountMinor = expense.amountMinor, splitMethod = expense.splitMethod,
                payments = expense.payments, shares = expense.shares,
                updatedAtMillis = System.currentTimeMillis(),
            ) else current
        }
        addActivity(expense.groupId, expenseId, ExpenseActivityAction.EXPENSE_UPDATED, expense.title)
        markSynced()
    }

    override suspend fun deleteExpense(expenseId: String) {
        val now = System.currentTimeMillis()
        val expense = requireNotNull(_expenses.value.firstOrNull { it.id == expenseId })
        _expenses.value = _expenses.value.map { if (it.id == expenseId) it.copy(deletedAtMillis = now) else it }
        addActivity(expense.groupId, expense.id, ExpenseActivityAction.EXPENSE_DELETED, expense.title)
        markSynced()
    }

    override suspend fun addComment(expenseId: String, body: String) {
        val user = requireUser()
        val clean = body.trim()
        require(clean.isNotEmpty() && clean.length <= 500) { "Write a comment of up to 500 characters." }
        _comments.value += ExpenseComment(
            UUID.randomUUID().toString(), expenseId, user.id, user.displayName, clean,
            System.currentTimeMillis(),
        )
        markSynced()
    }

    override suspend fun deleteComment(commentId: String) {
        _comments.value = _comments.value.filterNot { it.id == commentId }
        markSynced()
    }

    override suspend fun recordSettlement(
        groupId: String,
        payeeId: String,
        amountMinor: Long,
        currency: String,
        note: String,
    ) {
        val user = requireUser()
        require(amountMinor > 0) { "Enter a valid settlement amount." }
        val payee = requireNotNull(_members.value.firstOrNull { it.groupId == groupId && it.userId == payeeId })
        val settlement = ExpenseSettlement(
            id = UUID.randomUUID().toString(), groupId = groupId, payerId = user.id,
            payeeId = payeeId, payerName = user.displayName, payeeName = payee.displayName,
            amountMinor = amountMinor, currency = currency, note = note.trim(),
            status = ExpenseSettlementStatus.PENDING, createdAtMillis = System.currentTimeMillis(),
        )
        _settlements.value += settlement
        addActivity(groupId, settlement.id, ExpenseActivityAction.SETTLEMENT_RECORDED, "${user.displayName} → ${payee.displayName}")
        markSynced()
    }

    override suspend fun confirmSettlement(settlementId: String) {
        val now = System.currentTimeMillis()
        val settlement = requireNotNull(_settlements.value.firstOrNull { it.id == settlementId })
        _settlements.value = _settlements.value.map {
            if (it.id == settlementId) it.copy(status = ExpenseSettlementStatus.CONFIRMED, confirmedAtMillis = now) else it
        }
        addActivity(settlement.groupId, settlement.id, ExpenseActivityAction.SETTLEMENT_CONFIRMED, "Payment confirmed")
        markSynced()
    }

    private fun addActivity(groupId: String, subjectId: String, action: ExpenseActivityAction, summary: String) {
        val user = requireUser()
        _activity.value = listOf(
            ExpenseActivity(
                UUID.randomUUID().toString(), groupId, user.id, user.displayName, action,
                subjectId, summary, System.currentTimeMillis(),
            ),
        ) + _activity.value
    }

    private fun requireUser() = requireNotNull(authRepository.session.value.user) { "Sign in to continue." }
    private fun markSynced() {
        _syncState.value = ExpenseSyncState(isCloudBacked = false, lastSyncedAtMillis = System.currentTimeMillis())
    }
}
