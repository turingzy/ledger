package cn.turingzy.ledger.ui.component.biz

import androidx.compose.runtime.staticCompositionLocalOf
import cn.turingzy.ledger.util.AmountFormatter
import cn.turingzy.ledger.model.settings.supportedCurrencyCodes
import java.util.Currency
import java.util.Locale

/**
 * 提供当前应用全局主货币代码的 [androidx.compose.runtime.CompositionLocal]，默认值为 "CNY"。
 */
internal val LocalCurrencyBiz = staticCompositionLocalOf { "CNY" }

/**
 * 获取指定货币代码对应的显示符号。
 *
 * 常用货币（如 CNY、USD）使用标准符号；对于其他支持的货币，获取其 Locale.US 符号；
 * 若符号与货币代码相同或为不支持的货币，则追加空格后缀以保持文本间距。
 *
 * @param currency 货币代码（如 "CNY"、"USD"）。
 * @return 对应的货币符号字符串。
 */
internal fun currencySymbolBiz(currency: String): String = when (currency) {
    "CNY" -> "¥"
    "USD" -> "$"
    else -> if (currency in supportedCurrencyCodes) {
        Currency.getInstance(currency).getSymbol(Locale.US).let { if (it == currency) "$it " else it }
    } else "$currency "
}

/**
 * 格式化交易金额字符串。
 *
 * 当交易币种与主货币一致时，带货币符号前缀并启用千分位分隔符；当不一致时，带货币代码前缀。
 *
 * @param amount 以“分”为单位的金额数值。
 * @param currency 交易对应的货币代码。
 * @param primary 当前主货币代码。
 * @return 格式化后的交易金额字符串。
 */
internal fun transactionAmountBiz(amount: Long, currency: String, primary: String): String =
    if (currency == primary) AmountFormatter.formatCents(amount, true, currencySymbolBiz(currency))
    else "$currency " + AmountFormatter.formatCents(amount)
