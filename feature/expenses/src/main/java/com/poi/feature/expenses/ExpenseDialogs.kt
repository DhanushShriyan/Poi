package com.poi.feature.expenses

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.poi.core.model.EventExpense
import com.poi.core.model.EventExpenseGroup
import com.poi.core.model.ExpenseAllocation
import com.poi.core.model.ExpenseCategory
import com.poi.core.model.ExpenseMember
import com.poi.core.model.ExpenseSplitMethod
import com.poi.core.model.NewEventExpense
import com.poi.core.model.SuggestedSettlement
import com.poi.core.model.allocateEvenly
import java.math.BigDecimal
import java.math.RoundingMode

internal data class ExpenseDraft(
    val expense: NewEventExpense,
    val receipt: ByteArray?,
    val receiptMimeType: String?,
)

@Composable
internal fun ExpenseEditorDialog(
    group: EventExpenseGroup,
    members: List<ExpenseMember>,
    currentUserId: String,
    initial: EventExpense?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (ExpenseDraft) -> Unit,
) {
    val context = LocalContext.current
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var amount by remember(initial?.id) {
        mutableStateOf(initial?.amountMinor?.let { "%.2f".format(it / 100.0) }.orEmpty())
    }
    var note by remember(initial?.id) { mutableStateOf(initial?.note.orEmpty()) }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: ExpenseCategory.OTHER) }
    var splitMethod by remember(initial?.id) { mutableStateOf(initial?.splitMethod ?: ExpenseSplitMethod.EQUAL) }
    val selectedPayers = remember(initial?.id, members) {
        mutableStateMapOf<String, Boolean>().apply {
            members.forEach { member ->
                put(member.userId, initial?.payments?.any { it.userId == member.userId } ?: (member.userId == currentUserId))
            }
        }
    }
    val selectedParticipants = remember(initial?.id, members) {
        mutableStateMapOf<String, Boolean>().apply {
            members.forEach { member ->
                put(member.userId, initial?.shares?.any { it.userId == member.userId && it.amountMinor > 0 } ?: true)
            }
        }
    }
    val splitValues = remember(initial?.id, members, splitMethod) {
        mutableStateMapOf<String, String>().apply {
            members.forEach { member ->
                val share = initial?.shares?.firstOrNull { it.userId == member.userId }?.amountMinor
                put(
                    member.userId,
                    when (splitMethod) {
                        ExpenseSplitMethod.EXACT -> share?.let { "%.2f".format(it / 100.0) }.orEmpty()
                        ExpenseSplitMethod.PERCENTAGE -> if (share != null && initial.amountMinor > 0) {
                            BigDecimal(share).multiply(BigDecimal(100))
                                .divide(BigDecimal(initial.amountMinor), 2, RoundingMode.HALF_UP)
                                .stripTrailingZeros().toPlainString()
                        } else ""
                        ExpenseSplitMethod.SHARES -> share?.takeIf { it > 0 }?.toString() ?: "1"
                        ExpenseSplitMethod.EQUAL -> ""
                    },
                )
            }
        }
    }
    var receiptBytes by remember(initial?.id) { mutableStateOf<ByteArray?>(null) }
    var receiptMimeType by remember(initial?.id) { mutableStateOf<String?>(null) }
    var receiptName by remember(initial?.id) { mutableStateOf<String?>(null) }
    var error by remember(initial?.id) { mutableStateOf<String?>(null) }
    val receiptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("The receipt could not be read.")
                require(bytes.size <= 6 * 1024 * 1024) { "Receipt photos must be 6 MB or smaller." }
                val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                require(mime in setOf("image/jpeg", "image/png", "image/webp")) {
                    "Use a JPEG, PNG, or WebP receipt photo."
                }
                receiptBytes = bytes
                receiptMimeType = mime
                receiptName = uri.lastPathSegment ?: "Receipt selected"
                error = null
            }.onFailure { error = it.message }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add expense" else "Edit expense") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(100) },
                    label = { Text("What was this for?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' || char == ',' }.take(16) },
                    label = { Text("Amount (${group.currency})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Category", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(ExpenseCategory.entries) { item ->
                        FilterChip(
                            selected = category == item,
                            onClick = { category = item },
                            label = { Text("${item.symbol} ${item.label}") },
                        )
                    }
                }
                Text("Who paid?", style = MaterialTheme.typography.labelLarge)
                members.forEach { member ->
                    SelectableMemberRow(
                        member = member,
                        selected = selectedPayers[member.userId] == true,
                        onToggle = { selectedPayers[member.userId] = it },
                    )
                }
                Text("Split method", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(ExpenseSplitMethod.entries) { method ->
                        FilterChip(
                            selected = splitMethod == method,
                            onClick = { splitMethod = method },
                            label = { Text(method.label) },
                        )
                    }
                }
                Text("Who shares this?", style = MaterialTheme.typography.labelLarge)
                members.forEach { member ->
                    Column {
                        SelectableMemberRow(
                            member = member,
                            selected = selectedParticipants[member.userId] == true,
                            onToggle = { selectedParticipants[member.userId] = it },
                        )
                        if (selectedParticipants[member.userId] == true && splitMethod != ExpenseSplitMethod.EQUAL) {
                            OutlinedTextField(
                                value = splitValues[member.userId].orEmpty(),
                                onValueChange = { splitValues[member.userId] = it.filter { char -> char.isDigit() || char == '.' }.take(14) },
                                label = {
                                    Text(
                                        when (splitMethod) {
                                            ExpenseSplitMethod.EXACT -> "${member.displayName}'s amount"
                                            ExpenseSplitMethod.PERCENTAGE -> "${member.displayName}'s percentage"
                                            ExpenseSplitMethod.SHARES -> "${member.displayName}'s shares"
                                            ExpenseSplitMethod.EQUAL -> ""
                                        },
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().padding(start = 36.dp),
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                if (initial == null) {
                    OutlinedButton(onClick = { receiptLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (receiptName == null) "Attach receipt photo" else "Change receipt")
                    }
                    receiptName?.let { Text(it, style = MaterialTheme.typography.labelMedium) }
                } else if (initial.receiptPath != null) {
                    Text("The existing receipt will remain attached.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    runCatching {
                        val total = requireNotNull(parseMoneyToMinor(amount)) { "Enter a valid amount." }
                        require(title.trim().isNotEmpty()) { "Give this expense a name." }
                        val payerIds = members.map(ExpenseMember::userId).filter { selectedPayers[it] == true }
                        require(payerIds.isNotEmpty()) { "Choose at least one payer." }
                        val participantIds = members.map(ExpenseMember::userId).filter { selectedParticipants[it] == true }
                        require(participantIds.isNotEmpty()) { "Choose at least one participant." }
                        val shares = when (splitMethod) {
                            ExpenseSplitMethod.EQUAL -> allocateEvenly(total, participantIds)
                            ExpenseSplitMethod.EXACT -> {
                                val allocations = participantIds.map { id ->
                                    ExpenseAllocation(
                                        id,
                                        requireNotNull(parseMoneyToMinor(splitValues[id].orEmpty())) {
                                            "Enter every exact amount."
                                        },
                                    )
                                }
                                require(allocations.sumOf(ExpenseAllocation::amountMinor) == total) {
                                    "Exact shares must equal ${moneyText(total, group.currency)}."
                                }
                                allocations
                            }
                            ExpenseSplitMethod.PERCENTAGE -> {
                                val weights = participantIds.associateWith { id ->
                                    splitValues[id].orEmpty().toBigDecimalOrNull()
                                        ?: error("Enter every percentage.")
                                }
                                val percentageTotal = weights.values.fold(BigDecimal.ZERO, BigDecimal::add)
                                require(percentageTotal.subtract(BigDecimal(100)).abs() <= BigDecimal("0.01")) {
                                    "Percentages must total 100."
                                }
                                allocateByWeights(total, weights)
                            }
                            ExpenseSplitMethod.SHARES -> {
                                val weights = participantIds.associateWith { id ->
                                    splitValues[id].orEmpty().toBigDecimalOrNull()
                                        ?: error("Enter every share value.")
                                }
                                allocateByWeights(total, weights)
                            }
                        }
                        ExpenseDraft(
                            NewEventExpense(
                                groupId = group.id, title = title.trim(), note = note.trim(),
                                category = category, currency = group.currency, amountMinor = total,
                                splitMethod = splitMethod, payments = allocateEvenly(total, payerIds), shares = shares,
                            ),
                            receiptBytes,
                            receiptMimeType,
                        )
                    }.onSuccess(onSave).onFailure { error = it.message ?: "Review the expense details." }
                },
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}

@Composable
private fun SelectableMemberRow(member: ExpenseMember, selected: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onToggle(!selected) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(selected, onCheckedChange = onToggle)
        Text(member.displayName)
    }
}

@Composable
internal fun SettlementDialog(
    suggestion: SuggestedSettlement,
    currency: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Long, String) -> Unit,
) {
    var amount by remember { mutableStateOf("%.2f".format(suggestion.amountMinor / 100.0)) }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${suggestion.payer.displayName} paid ${suggestion.payee.displayName}")
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' }.take(16) },
                    label = { Text("Amount ($currency)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(300) },
                    label = { Text("Payment note (optional)") },
                )
                Text(
                    "The recipient must confirm this payment before balances change.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(
                enabled = !saving,
                onClick = {
                    val parsed = parseMoneyToMinor(amount)
                    if (parsed == null) error = "Enter a valid payment amount."
                    else onConfirm(parsed, note)
                },
            ) { Text(if (saving) "Saving…" else "Record paid") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun UpiIdDialog(
    initial: String,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("UPI payment handoff") },
        text = {
            Column {
                Text(
                    "Your UPI ID is visible only to accepted members of this event expense group. Poi never receives or holds the payment.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(100) },
                    label = { Text("UPI ID, for example name@bank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(value.ifBlank { null }) }, enabled = !saving) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
