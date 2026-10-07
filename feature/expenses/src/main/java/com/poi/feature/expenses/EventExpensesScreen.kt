package com.poi.feature.expenses

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.poi.core.data.ExpenseRepository
import com.poi.core.data.SocialRepository
import com.poi.core.designsystem.PoiInitialAvatar
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.designsystem.PoiStatusPill
import com.poi.core.model.EventExpense
import com.poi.core.model.EventExpenseGroup
import com.poi.core.model.ExpenseActivity
import com.poi.core.model.ExpenseBalance
import com.poi.core.model.ExpenseCategory
import com.poi.core.model.ExpenseComment
import com.poi.core.model.ExpenseMember
import com.poi.core.model.ExpenseMemberStatus
import com.poi.core.model.ExpenseSettlement
import com.poi.core.model.ExpenseSettlementStatus
import com.poi.core.model.FriendshipStatus
import com.poi.core.model.SuggestedSettlement
import com.poi.core.model.calculateExpenseBalances
import com.poi.core.model.simplifyExpenseBalances
import java.text.DateFormat
import java.util.Locale
import java.util.Date
import kotlinx.coroutines.launch

private enum class ExpenseTab(val label: String) {
    EXPENSES("Expenses"),
    BALANCES("Balances"),
    PEOPLE("People"),
    ACTIVITY("Activity"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventExpensesScreen(
    eventId: String,
    eventTitle: String,
    currentUserId: String,
    repository: ExpenseRepository,
    socialRepository: SocialRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val groups by repository.groups.collectAsStateWithLifecycle()
    val allMembers by repository.members.collectAsStateWithLifecycle()
    val allExpenses by repository.expenses.collectAsStateWithLifecycle()
    val allComments by repository.comments.collectAsStateWithLifecycle()
    val allSettlements by repository.settlements.collectAsStateWithLifecycle()
    val allActivity by repository.activity.collectAsStateWithLifecycle()
    val syncState by repository.syncState.collectAsStateWithLifecycle()
    val friendships by socialRepository.friendships.collectAsStateWithLifecycle()
    var selectedGroupId by remember(eventId, currentUserId) { mutableStateOf<String?>(null) }
    val eventGroups = groups.filter { it.eventId == eventId }
    val group = eventGroups.firstOrNull { it.id == selectedGroupId }
        ?: eventGroups.firstOrNull()
    val members = group?.let { selected -> allMembers.filter { it.groupId == selected.id } }.orEmpty()
    val activeMembers = members.filter { it.status == ExpenseMemberStatus.ACTIVE }
    val expenses = group?.let { selected -> allExpenses.filter { it.groupId == selected.id } }.orEmpty()
    val comments = allComments.filter { comment -> expenses.any { it.id == comment.expenseId } }
    val settlements = group?.let { selected -> allSettlements.filter { it.groupId == selected.id } }.orEmpty()
    val activity = group?.let { selected -> allActivity.filter { it.groupId == selected.id } }.orEmpty()
    val currentMember = members.firstOrNull { it.userId == currentUserId }
    val balances = remember(activeMembers, expenses, settlements) {
        calculateExpenseBalances(activeMembers, expenses, settlements)
    }
    val suggestions = remember(balances) { simplifyExpenseBalances(balances) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by remember { mutableStateOf(ExpenseTab.EXPENSES) }
    var query by remember { mutableStateOf("") }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<EventExpense?>(null) }
    var settlementSuggestion by remember { mutableStateOf<SuggestedSettlement?>(null) }
    var showUpiDialog by remember { mutableStateOf(false) }
    var expandedExpenseId by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUserId) {
        runCatching { repository.refresh() }.onFailure { message = friendlyExpenseError(it) }
    }

    fun runAction(action: suspend () -> Unit) {
        busy = true
        message = null
        scope.launch {
            runCatching { action() }
                .onFailure { message = friendlyExpenseError(it) }
            busy = false
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Event expenses")
                        Text(
                            eventTitle,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (group != null && currentMember?.status == ExpenseMemberStatus.ACTIVE) {
                        IconButton(
                            onClick = {
                                val export = buildExpenseExport(eventTitle, group, activeMembers, expenses, balances)
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/csv"
                                            putExtra(Intent.EXTRA_SUBJECT, "$eventTitle expense statement")
                                            putExtra(Intent.EXTRA_TEXT, export)
                                        },
                                        "Export expense statement",
                                    ),
                                )
                            },
                        ) { Icon(Icons.Default.Share, "Export statement") }
                    }
                },
            )
        },
        floatingActionButton = {
            if (group != null && currentMember?.status == ExpenseMemberStatus.ACTIVE && tab == ExpenseTab.EXPENSES) {
                ExtendedFloatingActionButton(
                    onClick = { editingExpense = null; showExpenseDialog = true },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Add expense") },
                )
            }
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (eventGroups.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(eventGroups, key = EventExpenseGroup::id) { candidate ->
                        val owner = allMembers.firstOrNull {
                            it.groupId == candidate.id && it.userId == candidate.createdBy
                        }
                        FilterChip(
                            selected = candidate.id == group?.id,
                            onClick = { selectedGroupId = candidate.id },
                            label = { Text(if (candidate.createdBy == currentUserId) "My circle" else "${owner?.displayName ?: "Friend"}'s circle") },
                        )
                    }
                    if (eventGroups.none { it.createdBy == currentUserId }) {
                        item {
                            OutlinedButton(enabled = !busy, onClick = {
                                runAction { selectedGroupId = repository.startGroup(eventId).id }
                            }) { Text("New circle") }
                        }
                    }
                }
            }
            Box(Modifier.weight(1f)) {
        when {
            group == null -> ExpenseOnboarding(
                eventTitle = eventTitle,
                loading = busy || syncState.isLoading,
                message = message ?: syncState.errorMessage,
                modifier = Modifier.fillMaxSize(),
                onStart = { runAction { repository.startGroup(eventId) } },
            )
            currentMember?.status == ExpenseMemberStatus.INVITED -> ExpenseInvitation(
                group = group,
                loading = busy,
                modifier = Modifier.fillMaxSize(),
                onAccept = { runAction { repository.respondToInvitation(group.id, true) } },
                onDecline = { runAction { repository.respondToInvitation(group.id, false) } },
            )
            currentMember?.status != ExpenseMemberStatus.ACTIVE -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("This expense group is private to its accepted members.")
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ExpenseHero(
                        currency = group.currency,
                        totalSpent = expenses.filterNot(EventExpense::isDeleted).sumOf(EventExpense::amountMinor),
                        currentBalance = balances.firstOrNull { it.member.userId == currentUserId }?.amountMinor ?: 0,
                        memberCount = activeMembers.size,
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ExpenseTab.entries) { item ->
                            FilterChip(selected = tab == item, onClick = { tab = item }, label = { Text(item.label) })
                        }
                    }
                }
                (message ?: syncState.errorMessage)?.let { text ->
                    item { MessageCard(text, isError = true) }
                }
                when (tab) {
                    ExpenseTab.EXPENSES -> {
                        item {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it.take(80) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                placeholder = { Text("Search title, category or payer") },
                            )
                        }
                        val visible = filterExpenses(expenses, activeMembers, query)
                        if (visible.isEmpty()) {
                            item {
                                EmptyExpenseCard(
                                    if (query.isBlank()) "No expenses yet" else "No matching expenses",
                                    if (query.isBlank()) "Add tickets, food, travel or stay costs when your group is ready."
                                    else "Try a different word or clear the search.",
                                )
                            }
                        } else {
                            item { CategorySummary(visible, group.currency) }
                            items(visible, key = EventExpense::id) { expense ->
                                ExpenseCard(
                                    expense = expense,
                                    members = activeMembers,
                                    comments = comments.filter { it.expenseId == expense.id },
                                    currentUserId = currentUserId,
                                    canManage = expense.createdBy == currentUserId || group.createdBy == currentUserId,
                                    expanded = expandedExpenseId == expense.id,
                                    busy = busy,
                                    onExpand = {
                                        expandedExpenseId = if (expandedExpenseId == expense.id) null else expense.id
                                    },
                                    onEdit = { editingExpense = expense; showExpenseDialog = true },
                                    onDelete = { runAction { repository.deleteExpense(expense.id) } },
                                    onComment = { body -> runAction { repository.addComment(expense.id, body) } },
                                    onDeleteComment = { id -> runAction { repository.deleteComment(id) } },
                                )
                            }
                        }
                    }
                    ExpenseTab.BALANCES -> {
                        item {
                            BalancesSection(
                                balances = balances,
                                suggestions = suggestions,
                                settlements = settlements,
                                currency = group.currency,
                                currentUserId = currentUserId,
                                currentMember = currentMember,
                                busy = busy,
                                onSetUpi = { showUpiDialog = true },
                                onPay = { suggestion -> openUpi(context, eventTitle, suggestion, group.currency) },
                                onRecord = { settlementSuggestion = it },
                                onRemind = { suggestion -> shareReminder(context, eventTitle, suggestion, group.currency) },
                                onConfirm = { id -> runAction { repository.confirmSettlement(id) } },
                            )
                        }
                    }
                    ExpenseTab.PEOPLE -> {
                        item { PoiSectionHeader("Expense group") }
                        items(members.filter { it.status != ExpenseMemberStatus.DECLINED }, key = ExpenseMember::userId) { member ->
                            MemberCard(member, member.userId == group.createdBy)
                        }
                        if (group.createdBy == currentUserId) {
                            val invitedIds = members.mapTo(mutableSetOf(), ExpenseMember::userId)
                            val available = friendships.filter {
                                it.status == FriendshipStatus.ACCEPTED && it.profile.id !in invitedIds
                            }
                            item { PoiSectionHeader("Invite accepted friends") }
                            if (available.isEmpty()) {
                                item { MessageCard("Everyone available is already invited. Add more accepted friends from People.") }
                            } else {
                                items(available, key = { it.profile.id }) { friendship ->
                                    Card {
                                        Row(
                                            Modifier.fillMaxWidth().padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            PoiInitialAvatar(friendship.profile.displayName, Modifier.size(42.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(friendship.profile.displayName, fontWeight = FontWeight.SemiBold)
                                                Text(friendship.profile.handle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            TextButton(
                                                enabled = !busy,
                                                onClick = { runAction { repository.inviteMember(group.id, friendship.profile.id) } },
                                            ) { Text("Invite") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    ExpenseTab.ACTIVITY -> {
                        item { PoiSectionHeader("Group activity") }
                        if (activity.isEmpty()) item { MessageCard("Expense changes and confirmed payments will appear here.") }
                        else items(activity, key = ExpenseActivity::id) { ActivityCard(it) }
                    }
                }
            }
        }
            }
        }
    }

    if (showExpenseDialog && group != null) {
        ExpenseEditorDialog(
            group = group,
            members = activeMembers,
            currentUserId = currentUserId,
            initial = editingExpense,
            saving = busy,
            onDismiss = { showExpenseDialog = false; editingExpense = null },
            onSave = { draft ->
                runAction {
                    if (editingExpense == null) {
                        repository.createExpense(draft.expense, draft.receipt, draft.receiptMimeType)
                    } else {
                        repository.updateExpense(checkNotNull(editingExpense).id, draft.expense)
                    }
                    showExpenseDialog = false
                    editingExpense = null
                }
            },
        )
    }
    settlementSuggestion?.let { suggestion ->
        SettlementDialog(
            suggestion = suggestion,
            currency = group?.currency ?: "INR",
            saving = busy,
            onDismiss = { settlementSuggestion = null },
            onConfirm = { amount, note ->
                val selectedGroup = checkNotNull(group)
                runAction {
                    repository.recordSettlement(
                        selectedGroup.id, suggestion.payee.userId, amount, selectedGroup.currency, note,
                    )
                    settlementSuggestion = null
                }
            },
        )
    }
    if (showUpiDialog && group != null && currentMember != null) {
        UpiIdDialog(
            initial = currentMember.upiId.orEmpty(),
            saving = busy,
            onDismiss = { showUpiDialog = false },
            onSave = { upiId ->
                runAction {
                    repository.updateUpiId(group.id, upiId)
                    showUpiDialog = false
                }
            },
        )
    }
}

@Composable
private fun ExpenseOnboarding(
    eventTitle: String,
    loading: Boolean,
    message: String?,
    modifier: Modifier,
    onStart: () -> Unit,
) {
    Column(
        modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(84.dp).background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraLarge),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.AccountBalanceWallet, null, Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(20.dp))
        Text("Split the event, not the friendship", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Create a private expense group for $eventTitle. Track who paid, split every paise and settle outside Poi.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("Only accepted group members can see financial details.")
        }
        message?.let { Spacer(Modifier.height(12.dp)); Text(it, color = MaterialTheme.colorScheme.error) }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onStart, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
            Text(if (loading) "Starting…" else "Start event expenses")
        }
    }
}

@Composable
private fun ExpenseInvitation(
    group: EventExpenseGroup,
    loading: Boolean,
    modifier: Modifier,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Column(modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.ReceiptLong, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Text("Join this expense group?", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Accept to view private expenses and participate in balances. Currency: ${group.currency}.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Button(onClick = onAccept, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Accept invitation") }
        TextButton(onClick = onDecline, enabled = !loading, modifier = Modifier.fillMaxWidth()) { Text("Decline") }
    }
}

@Composable
private fun ExpenseHero(currency: String, totalSpent: Long, currentBalance: Long, memberCount: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PoiStatusPill("Private group")
                Spacer(Modifier.weight(1f))
                Text("$memberCount members", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(20.dp))
            Text("Total event spend", color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(moneyText(totalSpent, currency), style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(12.dp))
            Text(
                when {
                    currentBalance > 0 -> "You get back ${moneyText(currentBalance, currency)}"
                    currentBalance < 0 -> "You owe ${moneyText(-currentBalance, currency)}"
                    else -> "You are settled up"
                },
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CategorySummary(expenses: List<EventExpense>, currency: String) {
    val totals = expenses.filterNot(EventExpense::isDeleted).groupBy(EventExpense::category)
        .mapValues { (_, values) -> values.sumOf(EventExpense::amountMinor) }
        .entries.sortedByDescending { it.value }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(totals, key = { it.key.name }) { (category, total) ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "${category.symbol} ${category.label}  ${moneyText(total, currency)}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: EventExpense,
    members: List<ExpenseMember>,
    comments: List<ExpenseComment>,
    currentUserId: String,
    canManage: Boolean,
    expanded: Boolean,
    busy: Boolean,
    onExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
) {
    val names = members.associate { it.userId to it.displayName }
    var commentBody by remember(expense.id) { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium),
                    contentAlignment = Alignment.Center,
                ) { Text(expense.category.symbol, style = MaterialTheme.typography.titleLarge) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(expense.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "Paid by ${expense.payments.joinToString { names[it.userId] ?: "Member" }} · ${expense.splitMethod.label}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(moneyText(expense.amountMinor, expense.currency), fontWeight = FontWeight.Bold)
            }
            if (expanded) {
                Spacer(Modifier.height(14.dp))
                expense.note.takeIf(String::isNotBlank)?.let { Text(it); Spacer(Modifier.height(10.dp)) }
                expense.receiptUrl?.takeIf(String::isNotBlank)?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Receipt for ${expense.title}",
                        modifier = Modifier.fillMaxWidth().height(190.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Text("Payment", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                expense.payments.forEach { Text("${names[it.userId] ?: "Member"} paid ${moneyText(it.amountMinor, expense.currency)}") }
                Spacer(Modifier.height(8.dp))
                Text("Split", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                expense.shares.filter { it.amountMinor > 0 }.forEach {
                    Text("${names[it.userId] ?: "Member"} owes ${moneyText(it.amountMinor, expense.currency)}")
                }
                if (canManage) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onEdit, enabled = !busy) { Icon(Icons.Default.Edit, null); Text("Edit") }
                        TextButton(onClick = onDelete, enabled = !busy) { Icon(Icons.Default.Delete, null); Text("Delete") }
                    }
                }
                PoiSectionHeader("Comments")
                if (comments.isEmpty()) Text("No comments yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                comments.forEach { comment ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(comment.authorName, fontWeight = FontWeight.SemiBold)
                            Text(comment.body)
                        }
                        if (comment.authorId == currentUserId) {
                            IconButton(onClick = { onDeleteComment(comment.id) }, enabled = !busy) {
                                Icon(Icons.Default.Delete, "Delete comment")
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = commentBody,
                        onValueChange = { commentBody = it.take(500) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a comment") },
                    )
                    TextButton(
                        enabled = commentBody.isNotBlank() && !busy,
                        onClick = { onComment(commentBody); commentBody = "" },
                    ) { Text("Post") }
                }
            }
        }
    }
}

@Composable
private fun BalancesSection(
    balances: List<ExpenseBalance>,
    suggestions: List<SuggestedSettlement>,
    settlements: List<ExpenseSettlement>,
    currency: String,
    currentUserId: String,
    currentMember: ExpenseMember,
    busy: Boolean,
    onSetUpi: () -> Unit,
    onPay: (SuggestedSettlement) -> Unit,
    onRecord: (SuggestedSettlement) -> Unit,
    onRemind: (SuggestedSettlement) -> Unit,
    onConfirm: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Verified, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Your UPI handoff", fontWeight = FontWeight.Bold)
                    Text(currentMember.upiId ?: "Not set", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onSetUpi) { Text(if (currentMember.upiId == null) "Set" else "Change") }
            }
        }
        PoiSectionHeader("Balances")
        balances.forEach { balance ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                PoiInitialAvatar(balance.member.displayName, Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Text(balance.member.displayName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Text(
                    when {
                        balance.amountMinor > 0 -> "gets ${moneyText(balance.amountMinor, currency)}"
                        balance.amountMinor < 0 -> "owes ${moneyText(-balance.amountMinor, currency)}"
                        else -> "settled"
                    },
                    color = if (balance.amountMinor >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
        }
        PoiSectionHeader("Suggested settlements")
        if (suggestions.isEmpty()) MessageCard("Everyone is settled up.")
        suggestions.forEach { suggestion ->
            Card {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(
                        "${suggestion.payer.displayName} pays ${suggestion.payee.displayName}",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(moneyText(suggestion.amountMinor, currency), style = MaterialTheme.typography.titleLarge)
                    when (currentUserId) {
                        suggestion.payer.userId -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (!suggestion.payee.upiId.isNullOrBlank()) {
                                Button(onClick = { onPay(suggestion) }) { Text("Pay via UPI") }
                            }
                            OutlinedButton(onClick = { onRecord(suggestion) }) { Text("Record paid") }
                        }
                        suggestion.payee.userId -> OutlinedButton(onClick = { onRemind(suggestion) }) { Text("Send reminder") }
                    }
                }
            }
        }
        PoiSectionHeader("Settlement history")
        if (settlements.isEmpty()) MessageCard("Payments recorded by members will appear here for confirmation.")
        settlements.forEach { settlement ->
            Card {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${settlement.payerName} → ${settlement.payeeName}", fontWeight = FontWeight.Bold)
                        Text("${moneyText(settlement.amountMinor, settlement.currency)} · ${settlement.status.name.lowercase()}")
                    }
                    if (settlement.payeeId == currentUserId && settlement.status == ExpenseSettlementStatus.PENDING) {
                        Button(onClick = { onConfirm(settlement.id) }, enabled = !busy) { Text("Confirm") }
                    } else if (settlement.status == ExpenseSettlementStatus.CONFIRMED) {
                        Icon(Icons.Default.CheckCircle, "Confirmed", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCard(member: ExpenseMember, organizer: Boolean) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            PoiInitialAvatar(member.displayName, Modifier.size(44.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(member.displayName, fontWeight = FontWeight.Bold)
                Text(
                    if (organizer) "Expense organizer" else member.status.name.lowercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (member.status == ExpenseMemberStatus.ACTIVE) Icon(Icons.Default.Verified, "Active member", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ActivityCard(activity: ExpenseActivity) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text("${activity.actorName} ${activity.action.label}", fontWeight = FontWeight.SemiBold)
                Text(activity.summary, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(activity.createdAtMillis)),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun EmptyExpenseCard(title: String, message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ReceiptLong, null, Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(10.dp)); Text(title, style = MaterialTheme.typography.titleLarge)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MessageCard(message: String, isError: Boolean = false) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Text(
            message,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun filterExpenses(
    expenses: List<EventExpense>,
    members: List<ExpenseMember>,
    query: String,
): List<EventExpense> {
    val normalized = query.trim().lowercase()
    val names = members.associate { it.userId to it.displayName.lowercase() }
    return expenses.filterNot(EventExpense::isDeleted).filter { expense ->
        normalized.isBlank() || expense.title.lowercase().contains(normalized) ||
            expense.category.label.lowercase().contains(normalized) ||
            expense.payments.any { names[it.userId]?.contains(normalized) == true }
    }.sortedByDescending(EventExpense::createdAtMillis)
}

private fun buildExpenseExport(
    eventTitle: String,
    group: EventExpenseGroup,
    members: List<ExpenseMember>,
    expenses: List<EventExpense>,
    balances: List<ExpenseBalance>,
): String = buildString {
    val names = members.associate { it.userId to it.displayName }
    appendLine("Poi event expense statement")
    appendLine("Event,$eventTitle")
    appendLine("Currency,${group.currency}")
    appendLine()
    appendLine("Expense,Category,Amount,Paid by,Split with")
    expenses.filterNot(EventExpense::isDeleted).forEach { expense ->
        appendLine(
            listOf(
                expense.title, expense.category.label, "%.2f".format(expense.amountMinor / 100.0),
                expense.payments.joinToString(" + ") { names[it.userId].orEmpty() },
                expense.shares.filter { it.amountMinor > 0 }.joinToString(" + ") { names[it.userId].orEmpty() },
            ).joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" },
        )
    }
    appendLine()
    appendLine("Member,Balance")
    balances.forEach { appendLine("\"${it.member.displayName}\",${it.amountMinor / 100.0}") }
}

private fun openUpi(
    context: android.content.Context,
    eventTitle: String,
    suggestion: SuggestedSettlement,
    currency: String,
) {
    val upiId = suggestion.payee.upiId ?: return
    val uri = Uri.parse("upi://pay").buildUpon()
        .appendQueryParameter("pa", upiId)
        .appendQueryParameter("pn", suggestion.payee.displayName)
        .appendQueryParameter("am", String.format(Locale.US, "%.2f", suggestion.amountMinor / 100.0))
        .appendQueryParameter("cu", currency)
        .appendQueryParameter("tn", "Poi · $eventTitle")
        .build()
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}

private fun shareReminder(
    context: android.content.Context,
    eventTitle: String,
    suggestion: SuggestedSettlement,
    currency: String,
) {
    val message = "Hi ${suggestion.payer.displayName}, your remaining share for $eventTitle is ${moneyText(suggestion.amountMinor, currency)}. You can record it in Poi after paying."
    context.startActivity(
        Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, message) },
            "Send a friendly reminder",
        ),
    )
}

internal fun friendlyExpenseError(error: Throwable): String {
    val normalized = error.message.orEmpty().lowercase()
    return when {
        "unable to resolve host" in normalized || "failed to connect" in normalized || "timeout" in normalized ->
            "Poi could not reach the expense service. Check your connection and try again."
        "permission" in normalized || "row-level security" in normalized ->
            "You do not have permission for that expense-group action."
        "must equal" in normalized || "valid" in normalized || "choose" in normalized || "enter" in normalized ->
            error.message.orEmpty().lineSequence().first().take(180)
        else -> error.message?.lineSequence()?.firstOrNull()?.take(180)
            ?: "That expense update could not be completed."
    }
}
