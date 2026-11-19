package cn.turingzy.ledger.ui.component.biz

import androidx.compose.runtime.staticCompositionLocalOf
import cn.turingzy.ledger.util.AmountFormatter
import cn.turingzy.ledger.model.settings.supportedCurrencyCodes
import java.util.Currency
import java.util.Locale

internal val LocalCurrencyBiz = staticCompositionLocalOf { "CNY" }
internal fun currencySymbolBiz(currency: String): String = when (currency) {
    "CNY" -> "¥"
    "USD" -> "$"
    else -> if (currency in supportedCurrencyCodes) {
        Currency.getInstance(currency).getSymbol(Locale.US).let { if (it == currency) "$it " else it }
    } else "$currency "
}

internal fun transactionAmountBiz(amount: Long, currency: String, primary: String): String =
    if (currency == primary) AmountFormatter.formatCents(amount, true, currencySymbolBiz(currency))
    else "$currency " + AmountFormatter.formatCents(amount)
