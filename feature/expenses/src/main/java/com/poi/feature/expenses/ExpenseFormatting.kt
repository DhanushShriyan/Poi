package com.poi.feature.expenses

import com.poi.core.model.ExpenseAllocation
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

internal fun moneyText(amountMinor: Long, currency: String): String = runCatching {
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")).apply {
        this.currency = Currency.getInstance(currency)
        maximumFractionDigits = 2
    }.format(amountMinor / 100.0)
}.getOrElse { "$currency %.2f".format(amountMinor / 100.0) }

internal fun parseMoneyToMinor(value: String): Long? = runCatching {
    value.trim().replace(",", "").toBigDecimal()
        .movePointRight(2)
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()
        .takeIf { it > 0 }
}.getOrNull()

internal fun allocateByWeights(
    totalMinor: Long,
    weights: Map<String, BigDecimal>,
): List<ExpenseAllocation> {
    require(totalMinor > 0 && weights.isNotEmpty()) { "Enter a valid split." }
    require(weights.values.all { it >= BigDecimal.ZERO }) { "Split values cannot be negative." }
    val totalWeight = weights.values.fold(BigDecimal.ZERO, BigDecimal::add)
    require(totalWeight > BigDecimal.ZERO) { "At least one split value must be positive." }
    val provisional = weights.map { (userId, weight) ->
        val exact = BigDecimal(totalMinor).multiply(weight).divide(totalWeight, 12, RoundingMode.DOWN)
        WeightedAmount(userId, exact.setScale(0, RoundingMode.DOWN).longValueExact(), exact.remainder(BigDecimal.ONE))
    }.toMutableList()
    var remainder = totalMinor - provisional.sumOf(WeightedAmount::amount)
    provisional.sortedByDescending(WeightedAmount::fraction).forEach { item ->
        if (remainder > 0) {
            val index = provisional.indexOfFirst { it.userId == item.userId }
            provisional[index] = provisional[index].copy(amount = provisional[index].amount + 1)
            remainder--
        }
    }
    return provisional.map { ExpenseAllocation(it.userId, it.amount) }
}

private data class WeightedAmount(
    val userId: String,
    val amount: Long,
    val fraction: BigDecimal,
)
