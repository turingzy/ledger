package cn.turingzy.ledger.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import cn.turingzy.ledger.util.AmountFormatter

internal val LocalLedgerCurrency = staticCompositionLocalOf { "CNY" }
internal fun currencySymbol(currency: String): String = when (currency) {
    "CNY" -> "¥"
    "USD" -> "$"
    else -> "$currency "
}

internal fun transactionAmount(amount: Long, currency: String, primary: String): String =
    if (currency == primary) AmountFormatter.formatCents(amount, true, currencySymbol(primary))
    else "$currency " + AmountFormatter.formatCents(amount)
