package cn.turingzy.ledger.ui.component.biz

import java.text.DecimalFormat
import java.math.BigDecimal
import cn.turingzy.ledger.util.AmountFormatter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 金额展示尺寸规格枚举。
 */
enum class AmountSizeBiz {
    /** 小号金额尺寸。 */
    Small,
    /** 常规默认金额尺寸。 */
    Regular,
    /** 突出强调金额尺寸。 */
    Emphasis,
    /** 首页或看板超大焦点金额尺寸。 */
    Hero,
    /** 交易记录列表金额尺寸。 */
    Transaction,
}

/**
 * 业务金额文本展示组件。
 *
 * 遵循统一的货币符号、正负符号前缀、资产隐私隐藏以及收支类型颜色规范展示金额。
 * 当交易币种与主货币不一致时，货币代码后缀将以弱化样式呈现。
 *
 * @param amount 以“分”为单位的金额数值；为 null 时显示占位符破折号。
 * @param currency 金额对应的货币代码（如 "CNY"、"USD"）。
 * @param modifier 应用于文本组件的修饰符。
 * @param type 交易类型（支出、收入、转账），决定正负号及主题颜色。
 * @param primaryCurrency 当前应用的主货币代码，默认为 [LocalCurrencyBiz.current]。
 * @param hidden 是否隐藏金额（隐私模式，显示为 ****）。
 * @param useGrouping 数字是否使用千分位逗号分隔。
 * @param size 金额尺寸样式变体，参见 [AmountSizeBiz]。
 * @param textStyle 可选的自定义文本样式；为 null 时按 [size] 自动匹配全局规范。
 * @param contentColor 可选的自定义文本颜色；为 null 时按 [type] 和状态自动计算。
 */
@Composable
fun AmountTextBiz(
    amount: Long?,
    currency: String,
    modifier: Modifier = Modifier,
    type: TransactionType? = null,
    primaryCurrency: String = LocalCurrencyBiz.current,
    hidden: Boolean = false,
    useGrouping: Boolean = false,
    size: AmountSizeBiz = AmountSizeBiz.Regular,
    textStyle: TextStyle? = null,
    contentColor: Color? = null,
) {
    val colors = LedgerStyle.colors
    val color = contentColor ?: when {
        hidden || amount == null -> colors.textSecondary
        type == null -> colors.textPrimary
        else -> colors.transaction(type)
    }
    val style = textStyle ?: when (size) {
        AmountSizeBiz.Small -> LedgerStyle.typography.amountSmall
        AmountSizeBiz.Regular -> LedgerStyle.typography.amount
        AmountSizeBiz.Emphasis -> LedgerStyle.typography.amountEmphasis
        AmountSizeBiz.Hero -> LedgerStyle.typography.heroAmount
        AmountSizeBiz.Transaction -> LedgerStyle.typography.transactionAmount
    }
    val text = amountTextBiz(amount, currency, primaryCurrency, type, hidden, useGrouping)
    val label = buildAnnotatedString {
        append(text)
        if (amount != null && !hidden && currency != primaryCurrency) {
            addStyle(
                SpanStyle(
                    color = colors.currencyUnit,
                    fontSize = LedgerStyle.typography.caption.fontSize,
                    letterSpacing = LedgerStyle.typography.caption.letterSpacing,
                ),
                start = text.length - currency.length - 1, end = text.length,
            )
        }
    }
    Text(
        label,
        modifier, style = style, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}

/**
 * 格式化金额为带符号和货币标识的字符串。
 *
 * @param amount 以“分”为单位的金额数值。
 * @param currency 货币代码。
 * @param primaryCurrency 当前主货币代码。
 * @param type 交易类型（用于决定正负号）。
 * @param hidden 是否处于隐私隐藏状态。
 * @param useGrouping 是否使用千分位分隔符。
 * @return 格式化后的金额展示字符串。
 */
internal fun amountTextBiz(
    amount: Long?,
    currency: String,
    primaryCurrency: String,
    type: TransactionType? = null,
    hidden: Boolean = false,
    useGrouping: Boolean = false,
): String {
    if (hidden) return "****"
    if (amount == null) return "—"
    // abs(Long.MIN_VALUE) overflows; keep the magnitude in decimal form for this boundary.
    val number = if (amount == Long.MIN_VALUE) {
        val decimal = BigDecimal.valueOf(amount).abs().movePointLeft(2)
        if (useGrouping) DecimalFormat("#,##0.00").format(decimal) else decimal.toPlainString()
    } else {
        AmountFormatter.formatCents(kotlin.math.abs(amount), useGrouping = useGrouping)
    }
    val signedPrefix = when {
        amount < 0L -> "−"
        type == TransactionType.EXPENSE -> "−"
        type == TransactionType.INCOME -> "+"
        else -> ""
    }
    val value = if (currency == primaryCurrency) "${currencySymbolBiz(currency)}$number" else "$number $currency"
    return "$signedPrefix$value"
}
