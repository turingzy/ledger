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

enum class AmountSizeBiz { Small, Regular, Emphasis, Hero, Transaction }

/** Renders a transaction or balance amount using the shared currency and color conventions. */
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
