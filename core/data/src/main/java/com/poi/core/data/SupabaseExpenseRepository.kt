package com.poi.core.data

import com.poi.core.auth.AuthRepository
import com.poi.core.cloud.PoiCloudClient
import com.poi.core.model.EventExpense
import com.poi.core.model.EventExpenseGroup
import com.poi.core.model.ExpenseActivity
import com.poi.core.model.ExpenseActivityAction
import com.poi.core.model.ExpenseAllocation
import com.poi.core.model.ExpenseCategory
import com.poi.core.model.ExpenseComment
import com.poi.core.model.ExpenseMember
import com.poi.core.model.ExpenseMemberStatus
import com.poi.core.model.ExpenseSettlement
import com.poi.core.model.ExpenseSettlementStatus
import com.poi.core.model.ExpenseSplitMethod
import com.poi.core.model.ExpenseSyncState
import com.poi.core.model.NewEventExpense
import io.github.jan.supabase.annotations.SupabaseExperimental
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.selectAsFlow
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import java.util.UUID
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KProperty1

@OptIn(SupabaseExperimental::class)
class SupabaseExpenseRepository(
    private val cloud: PoiCloudClient,
    private val authRepository: AuthRepository,
) : ExpenseRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val groupRows = MutableStateFlow<List<ExpenseGroupRow>>(emptyList())
    private val memberRows = MutableStateFlow<List<ExpenseMemberRow>>(emptyList())
    private val expenseRows = MutableStateFlow<List<ExpenseRow>>(emptyList())
    private val paymentRows = MutableStateFlow<List<ExpenseAllocationRow>>(emptyList())
    private val shareRows = MutableStateFlow<List<ExpenseAllocationRow>>(emptyList())
    private val commentRows = MutableStateFlow<List<ExpenseCommentRow>>(emptyList())
    private val settlementRows = MutableStateFlow<List<ExpenseSettlementRow>>(emptyList())
    private val activityRows = MutableStateFlow<List<ExpenseActivityRow>>(emptyList())
    private val receiptUrls = mutableMapOf<String, String>()
    private val rebuildMutex = Mutex()

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
    private val _syncState = MutableStateFlow(ExpenseSyncState(isCloudBacked = true))
    override val syncState: StateFlow<ExpenseSyncState> = _syncState.asStateFlow()

    init {
        scope.launch {
            authRepository.session.collectLatest { session ->
                clear()
                if (!session.isAuthenticated) return@collectLatest
                coroutineScope {
                    launchRealtime("event_expense_groups", listOf(ExpenseGroupRow::id), groupRows)
                    launchRealtime("event_expense_members", listOf(ExpenseMemberRow::groupId, ExpenseMemberRow::userId), memberRows)
                    launchRealtime("event_expenses", listOf(ExpenseRow::id), expenseRows)
                    launchRealtime("event_expense_payments", listOf(ExpenseAllocationRow::expenseId, ExpenseAllocationRow::userId), paymentRows)
                    launchRealtime("event_expense_shares", listOf(ExpenseAllocationRow::expenseId, ExpenseAllocationRow::userId), shareRows)
                    launchRealtime("event_expense_comments", listOf(ExpenseCommentRow::id), commentRows)
                    launchRealtime("event_expense_settlements", listOf(ExpenseSettlementRow::id), settlementRows)
                    launchRealtime("event_expense_activity", listOf(ExpenseActivityRow::id), activityRows)
                }
            }
        }
    }

    override suspend fun refresh() = connectedOperation {
        groupRows.value = cloud.supabase.from("event_expense_groups").select().decodeList()
        memberRows.value = cloud.supabase.from("event_expense_members").select().decodeList()
        expenseRows.value = cloud.supabase.from("event_expenses").select().decodeList()
        paymentRows.value = cloud.supabase.from("event_expense_payments").select().decodeList()
        shareRows.value = cloud.supabase.from("event_expense_shares").select().decodeList()
        commentRows.value = cloud.supabase.from("event_expense_comments").select().decodeList()
        settlementRows.value = cloud.supabase.from("event_expense_settlements").select().decodeList()
        activityRows.value = cloud.supabase.from("event_expense_activity").select().decodeList()
        rebuild()
    }

    override suspend fun startGroup(eventId: String, currency: String): EventExpenseGroup = connectedOperation {
        groupRows.value.firstOrNull {
            it.eventId == eventId && it.createdBy == requireUserId()
        }?.toModel() ?: run {
            val userId = requireUserId()
            val row = cloud.supabase.from("event_expense_groups").insert(
                NewExpenseGroupRow(eventId = eventId, createdBy = userId, currency = currency),
            ) { select() }.decodeSingle<ExpenseGroupRow>()
            refresh()
            row.toModel()
        }
    }

    override suspend fun inviteMember(groupId: String, profileId: String) = connectedOperation {
        cloud.supabase.from("event_expense_members").insert(
            NewExpenseMemberRow(groupId, profileId, requireUserId()),
        )
        refresh()
    }

    override suspend fun respondToInvitation(groupId: String, accept: Boolean) = connectedOperation {
        cloud.supabase.from("event_expense_members").update(
            MembershipStatusUpdateRow(status = if (accept) "active" else "declined"),
        ) {
            filter {
                eq("group_id", groupId)
                eq("user_id", requireUserId())
            }
        }
        refresh()
    }

    override suspend fun updateUpiId(groupId: String, upiId: String?) = connectedOperation {
        cloud.supabase.from("event_expense_members").update(
            UpiIdUpdateRow(upiId = normalizeUpiId(upiId)),
        ) {
            filter {
                eq("group_id", groupId)
                eq("user_id", requireUserId())
            }
        }
        refresh()
    }

    override suspend fun createExpense(
        expense: NewEventExpense,
        receipt: ByteArray?,
        receiptMimeType: String?,
    ): EventExpense = connectedOperation {
        validateExpense(expense)
        validateReceipt(receipt, receiptMimeType)
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val receiptPath = receipt?.let { bytes ->
            val mimeType = checkNotNull(receiptMimeType)
            val path = "${expense.groupId}/${requireUserId()}/$id.${mimeType.fileExtension()}"
            cloud.supabase.storage.from(RECEIPT_BUCKET).upload(path, bytes) {
                upsert = false
                contentType = ContentType.parse(mimeType)
            }
            path
        }
        try {
            cloud.supabase.postgrest.rpc(
                "create_poi_event_expense",
                Json.encodeToJsonElement(
                    CreateExpenseParams.from(id, expense, receiptPath, now),
                ).jsonObject,
            )
        } catch (error: Throwable) {
            receiptPath?.let { path -> runCatching { cloud.supabase.storage.from(RECEIPT_BUCKET).delete(path) } }
            throw error
        }
        refresh()
        requireNotNull(_expenses.value.firstOrNull { it.id == id }) {
            "The expense was saved but could not be loaded. Refresh and try again."
        }
    }

    override suspend fun updateExpense(expenseId: String, expense: NewEventExpense) = connectedOperation {
        validateExpense(expense)
        cloud.supabase.postgrest.rpc(
            "update_poi_event_expense",
            Json.encodeToJsonElement(
                UpdateExpenseParams.from(expenseId, expense, System.currentTimeMillis()),
            ).jsonObject,
        )
        refresh()
    }

    override suspend fun deleteExpense(expenseId: String) = connectedOperation {
        cloud.supabase.postgrest.rpc(
            "delete_poi_event_expense",
            Json.encodeToJsonElement(DeleteExpenseParams(expenseId)).jsonObject,
        )
        refresh()
    }

    override suspend fun addComment(expenseId: String, body: String) = connectedOperation {
        val clean = body.trim()
        require(clean.isNotEmpty()) { "Write a comment first." }
        require(clean.length <= 500) { "Comments can contain up to 500 characters." }
        cloud.supabase.from("event_expense_comments").insert(
            NewExpenseCommentRow(expenseId = expenseId, body = clean),
        )
        refresh()
    }

    override suspend fun deleteComment(commentId: String) = connectedOperation {
        cloud.supabase.from("event_expense_comments").delete { filter { eq("id", commentId) } }
        refresh()
    }

    override suspend fun recordSettlement(
        groupId: String,
        payeeId: String,
        amountMinor: Long,
        currency: String,
        note: String,
    ) = connectedOperation {
        require(amountMinor > 0) { "Enter a valid settlement amount." }
        require(note.length <= 300) { "Settlement notes can contain up to 300 characters." }
        cloud.supabase.from("event_expense_settlements").insert(
            NewSettlementRow(
                groupId = groupId,
                payerId = requireUserId(),
                payeeId = payeeId,
                amountMinor = amountMinor,
                currency = currency,
                note = note.trim(),
            ),
        )
        refresh()
    }

    override suspend fun confirmSettlement(settlementId: String) = connectedOperation {
        cloud.supabase.from("event_expense_settlements").update(SettlementStatusUpdateRow("confirmed")) {
            filter { eq("id", settlementId) }
        }
        refresh()
    }

    private inline fun <reified T : Any, Value> CoroutineScope.launchRealtime(
        table: String,
        primaryKeys: List<KProperty1<T, Value>>,
        target: MutableStateFlow<List<T>>,
    ) = launch {
        cloud.supabase.from(table).selectAsFlow(primaryKeys)
            .onStart { _syncState.value = _syncState.value.copy(isLoading = true, errorMessage = null) }
            .retryWhen { cause, attempt ->
                markFailure(cause)
                delay((attempt + 1).coerceAtMost(6) * 1_000L)
                true
            }
            .catch { error -> markFailure(error) }
            .collectLatest { rows ->
                target.value = rows
                rebuild()
                markSuccess()
            }
    }

    private suspend fun rebuild() = rebuildMutex.withLock {
        _groups.value = groupRows.value.map(ExpenseGroupRow::toModel).sortedBy(EventExpenseGroup::createdAtMillis)
        _members.value = memberRows.value.map(ExpenseMemberRow::toModel).sortedBy(ExpenseMember::displayName)
        val payments = paymentRows.value.groupBy(ExpenseAllocationRow::expenseId)
        val shares = shareRows.value.groupBy(ExpenseAllocationRow::expenseId)
        _expenses.value = expenseRows.value.map { row ->
            val receiptUrl = row.receiptPath?.let { path ->
                runCatching {
                    cloud.supabase.storage.from(RECEIPT_BUCKET).createSignedUrl(path, 2.hours)
                }.getOrElse {
                    if (it is CancellationException) throw it
                    receiptUrls[path].orEmpty()
                }.also { url ->
                    if (url.isNotBlank()) receiptUrls[path] = url
                }
            }
            row.toModel(
                payments = payments[row.id].orEmpty().map(ExpenseAllocationRow::toModel),
                shares = shares[row.id].orEmpty().map(ExpenseAllocationRow::toModel),
                receiptUrl = receiptUrl,
            )
        }.sortedByDescending(EventExpense::createdAtMillis)
        currentCoroutineContext().ensureActive()
        _comments.value = commentRows.value.map(ExpenseCommentRow::toModel).sortedBy(ExpenseComment::createdAtMillis)
        _settlements.value = settlementRows.value.map(ExpenseSettlementRow::toModel)
            .sortedByDescending(ExpenseSettlement::createdAtMillis)
        _activity.value = activityRows.value.mapNotNull(ExpenseActivityRow::toModel)
            .sortedByDescending(ExpenseActivity::createdAtMillis)
    }

    private suspend fun <T> connectedOperation(block: suspend () -> T): T {
        _syncState.value = _syncState.value.copy(isLoading = true, errorMessage = null)
        return try {
            block().also { markSuccess() }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            markFailure(error)
            throw error
        }
    }

    private suspend fun clear() = rebuildMutex.withLock {
        groupRows.value = emptyList(); memberRows.value = emptyList(); expenseRows.value = emptyList()
        paymentRows.value = emptyList(); shareRows.value = emptyList(); commentRows.value = emptyList()
        settlementRows.value = emptyList(); activityRows.value = emptyList(); receiptUrls.clear()
        _groups.value = emptyList(); _members.value = emptyList(); _expenses.value = emptyList()
        _comments.value = emptyList(); _settlements.value = emptyList(); _activity.value = emptyList()
        _syncState.value = ExpenseSyncState(isCloudBacked = true)
    }

    private fun markSuccess() {
        _syncState.value = ExpenseSyncState(
            isCloudBacked = true,
            lastSyncedAtMillis = System.currentTimeMillis(),
        )
    }

    private fun markFailure(error: Throwable) {
        _syncState.value = ExpenseSyncState(
            isCloudBacked = true,
            errorMessage = serviceErrorMessage(error),
            lastSyncedAtMillis = _syncState.value.lastSyncedAtMillis,
        )
    }

    private fun requireUserId(): String =
        requireNotNull(authRepository.session.value.user?.id) { "Sign in to continue." }

    private fun validateReceipt(receipt: ByteArray?, mimeType: String?) {
        if (receipt == null) return
        require(receipt.isNotEmpty()) { "Choose a receipt photo." }
        require(receipt.size <= MAX_RECEIPT_BYTES) { "Receipt photos must be 6 MB or smaller." }
        require(mimeType in SUPPORTED_RECEIPT_TYPES) { "Use a JPEG, PNG, or WebP receipt photo." }
    }

    private companion object {
        const val RECEIPT_BUCKET = "event-expense-receipts"
        const val MAX_RECEIPT_BYTES = 6 * 1024 * 1024
        val SUPPORTED_RECEIPT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
}

@Serializable
private data class ExpenseGroupRow(
    val id: String,
    @SerialName("event_id") val eventId: String,
    @SerialName("created_by") val createdBy: String,
    val currency: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
) {
    fun toModel() = EventExpenseGroup(id, eventId, createdBy, currency, createdAtMillis)
}

@Serializable
private data class NewExpenseGroupRow(
    @SerialName("event_id") val eventId: String,
    @SerialName("created_by") val createdBy: String,
    val currency: String,
)

@Serializable
private data class ExpenseMemberRow(
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("display_name") val displayName: String,
    val status: String,
    @SerialName("upi_id") val upiId: String? = null,
    @SerialName("invited_by") val invitedBy: String? = null,
) {
    val compoundKey: String get() = "$groupId:$userId"

    fun toModel() = ExpenseMember(
        groupId, userId, displayName,
        enumValueOrDefault(status, ExpenseMemberStatus.INVITED), upiId, invitedBy,
    )
}

@Serializable
private data class NewExpenseMemberRow(
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("invited_by") val invitedBy: String,
)

@Serializable
private data class MembershipStatusUpdateRow(
    val status: String,
)

@Serializable
private data class UpiIdUpdateRow(
    @SerialName("upi_id") val upiId: String? = null,
)

@Serializable
private data class ExpenseRow(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_by_name") val createdByName: String,
    val title: String,
    val note: String,
    val category: String,
    val currency: String,
    @SerialName("amount_minor") val amountMinor: Long,
    @SerialName("split_method") val splitMethod: String,
    @SerialName("receipt_path") val receiptPath: String? = null,
    @SerialName("created_at_millis") val createdAtMillis: Long,
    @SerialName("updated_at_millis") val updatedAtMillis: Long,
    @SerialName("deleted_at_millis") val deletedAtMillis: Long? = null,
) {
    fun toModel(
        payments: List<ExpenseAllocation>,
        shares: List<ExpenseAllocation>,
        receiptUrl: String?,
    ) = EventExpense(
        id, groupId, createdBy, createdByName, title, note,
        enumValueOrDefault(category, ExpenseCategory.OTHER), currency, amountMinor,
        enumValueOrDefault(splitMethod, ExpenseSplitMethod.EQUAL), receiptPath, receiptUrl,
        payments, shares, createdAtMillis, updatedAtMillis, deletedAtMillis,
    )
}

@Serializable
private data class ExpenseAllocationRow(
    @SerialName("expense_id") val expenseId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("amount_minor") val amountMinor: Long,
) {
    val compoundKey: String get() = "$expenseId:$userId"
    fun toModel() = ExpenseAllocation(userId, amountMinor)
}

@Serializable
private data class AllocationParam(
    @SerialName("user_id") val userId: String,
    @SerialName("amount_minor") val amountMinor: Long,
)

@Serializable
private data class CreateExpenseParams(
    @SerialName("p_expense_id") val expenseId: String,
    @SerialName("p_group_id") val groupId: String,
    @SerialName("p_title") val title: String,
    @SerialName("p_note") val note: String,
    @SerialName("p_category") val category: String,
    @SerialName("p_currency") val currency: String,
    @SerialName("p_amount_minor") val amountMinor: Long,
    @SerialName("p_split_method") val splitMethod: String,
    @SerialName("p_payments") val payments: List<AllocationParam>,
    @SerialName("p_shares") val shares: List<AllocationParam>,
    @SerialName("p_receipt_path") val receiptPath: String?,
    @SerialName("p_created_at_millis") val createdAtMillis: Long,
) {
    companion object {
        fun from(id: String, expense: NewEventExpense, receiptPath: String?, now: Long) = CreateExpenseParams(
            id, expense.groupId, expense.title.trim(), expense.note.trim(), expense.category.name.lowercase(),
            expense.currency, expense.amountMinor, expense.splitMethod.name.lowercase(),
            expense.payments.map { AllocationParam(it.userId, it.amountMinor) },
            expense.shares.map { AllocationParam(it.userId, it.amountMinor) }, receiptPath, now,
        )
    }
}

@Serializable
private data class UpdateExpenseParams(
    @SerialName("p_expense_id") val expenseId: String,
    @SerialName("p_title") val title: String,
    @SerialName("p_note") val note: String,
    @SerialName("p_category") val category: String,
    @SerialName("p_amount_minor") val amountMinor: Long,
    @SerialName("p_split_method") val splitMethod: String,
    @SerialName("p_payments") val payments: List<AllocationParam>,
    @SerialName("p_shares") val shares: List<AllocationParam>,
    @SerialName("p_updated_at_millis") val updatedAtMillis: Long,
) {
    companion object {
        fun from(id: String, expense: NewEventExpense, now: Long) = UpdateExpenseParams(
            id, expense.title.trim(), expense.note.trim(), expense.category.name.lowercase(),
            expense.amountMinor, expense.splitMethod.name.lowercase(),
            expense.payments.map { AllocationParam(it.userId, it.amountMinor) },
            expense.shares.map { AllocationParam(it.userId, it.amountMinor) }, now,
        )
    }
}

@Serializable
private data class DeleteExpenseParams(@SerialName("p_expense_id") val expenseId: String)

@Serializable
private data class ExpenseCommentRow(
    val id: String,
    @SerialName("expense_id") val expenseId: String,
    @SerialName("author_id") val authorId: String,
    @SerialName("author_name") val authorName: String,
    val body: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
) {
    fun toModel() = ExpenseComment(id, expenseId, authorId, authorName, body, createdAtMillis)
}

@Serializable
private data class NewExpenseCommentRow(
    @SerialName("expense_id") val expenseId: String,
    val body: String,
)

@Serializable
private data class ExpenseSettlementRow(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("payer_id") val payerId: String,
    @SerialName("payee_id") val payeeId: String,
    @SerialName("payer_name") val payerName: String,
    @SerialName("payee_name") val payeeName: String,
    @SerialName("amount_minor") val amountMinor: Long,
    val currency: String,
    val note: String,
    val status: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
    @SerialName("confirmed_at_millis") val confirmedAtMillis: Long? = null,
) {
    fun toModel() = ExpenseSettlement(
        id, groupId, payerId, payeeId, payerName, payeeName, amountMinor, currency, note,
        enumValueOrDefault(status, ExpenseSettlementStatus.PENDING), createdAtMillis, confirmedAtMillis,
    )
}

@Serializable
private data class NewSettlementRow(
    @SerialName("group_id") val groupId: String,
    @SerialName("payer_id") val payerId: String,
    @SerialName("payee_id") val payeeId: String,
    @SerialName("amount_minor") val amountMinor: Long,
    val currency: String,
    val note: String,
)

@Serializable
private data class SettlementStatusUpdateRow(val status: String)

@Serializable
private data class ExpenseActivityRow(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("actor_id") val actorId: String? = null,
    @SerialName("actor_name") val actorName: String,
    val action: String,
    @SerialName("subject_id") val subjectId: String,
    val summary: String,
    @SerialName("created_at_millis") val createdAtMillis: Long,
) {
    fun toModel(): ExpenseActivity? = enumValueOrNull<ExpenseActivityAction>(action)?.let { parsed ->
        ExpenseActivity(id, groupId, actorId, actorName, parsed, subjectId, summary, createdAtMillis)
    }
}

private fun String.fileExtension(): String = when (this) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    else -> "jpg"
}

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, fallback: T): T =
    enumValueOrNull<T>(value) ?: fallback
