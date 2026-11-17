package cn.turingzy.ledger.ui.component.biz

import androidx.compose.runtime.staticCompositionLocalOf
import cn.turingzy.ledger.util.AmountFormatter
import cn.turingzy.ledger.model.settings.supportedCurrencyCodes
import java.util.Currency
import java.util.Locale

internal val LocalLedgerCurrency = staticCompositionLocalOf { "CNY" }
internal fun currencySymbol(currency: String): String = when (currency) {
    "CNY" -> "¥"
    "USD" -> "$"
    else -> if (currency in supportedCurrencyCodes) {
        Currency.getInstance(currency).getSymbol(Locale.US).let { if (it == currency) "$it " else it }
    } else "$currency "
}

internal fun transactionAmount(amount: Long, currency: String, primary: String): String =
    if (currency == primary) AmountFormatter.formatCents(amount, true, currencySymbol(currency))
    else "$currency " + AmountFormatter.formatCents(amount)
