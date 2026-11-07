package cn.turingzy.ledger.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

object AmountFormatter {

    private val decimalFormat = DecimalFormat("#,##0.00")

    fun formatCents(
        cents: Long,
        showSymbol: Boolean = false,
        symbol: String = "¥",
        useGrouping: Boolean = false,
    ): String {
        val isNegative = cents < 0
        val absCents = if (isNegative) -cents else cents
        val yuan = absCents / 100
        val remainder = absCents % 100

        val numberString = if (useGrouping) {
            val bd = BigDecimal(absCents).divide(BigDecimal(100), 2, RoundingMode.UNNECESSARY)
            decimalFormat.format(bd)
        } else {
            "$yuan.${remainder.toString().padStart(2, '0')}"
        }

        val prefix = when {
            isNegative && showSymbol -> "-$symbol"
            isNegative -> "-"
            showSymbol -> symbol
            else -> ""
        }

        return "$prefix$numberString"
    }

    fun parseToCents(amountStr: String): Long? {
        val cleanStr = amountStr.trim().removePrefix("¥").removePrefix("$").replace(",", "").trim()
        if (cleanStr.isEmpty()) return null

        return runCatching {
            val bd = BigDecimal(cleanStr).setScale(2, RoundingMode.HALF_UP)
            bd.movePointRight(2).longValueExact()
        }.getOrNull()
    }
}

fun Long.toFormattedAmount(
    showSymbol: Boolean = false,
    symbol: String = "¥",
    useGrouping: Boolean = false,
): String = AmountFormatter.formatCents(
    cents = this,
    showSymbol = showSymbol,
    symbol = symbol,
    useGrouping = useGrouping,
)

