package cn.turingzy.ledger.ui.components.business

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.categoryGlyph
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.component.LocalLedgerCurrency
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.interaction.uiClickable

@Composable
fun BusinessTransactionItem(
    amount: Long,
    currency: String,
    type: TransactionType,
    category: CategoryWithIcon?,
    accountName: String,
    timeText: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    primaryCurrency: String = LocalLedgerCurrency.current,
    hidden: Boolean = false,
    isDeleted: Boolean = false,
    accountDeleted: Boolean = false,
    destinationName: String? = null,
    destinationCurrency: String? = null,
    destinationDeleted: Boolean = false,
    receivedAmount: Long? = null,
    onClick: (() -> Unit)? = null,
) {
    if (isDeleted) return
    val deleted = stringResource(R.string.business_deleted)
    val unavailable = stringResource(R.string.business_unavailable_account)
    fun historicalName(name: String, archived: Boolean): String = if (archived) "$name ($deleted)" else name
    val source = historicalName(accountName, accountDeleted)
    val received = receivedAmount ?: amount.takeIf { destinationName != null && currency == destinationCurrency }
    val showReceived = type == TransactionType.TRANSFER && (
        destinationName == null || destinationCurrency != currency || received != amount
    )
    val supporting = buildList {
        add(if (type == TransactionType.TRANSFER) {
            val target = destinationName?.let { historicalName(it, destinationDeleted) } ?: unavailable
            "$source → $target"
        } else source)
        note?.takeIf { it.isNotBlank() }?.let(::add)
    }.joinToString(" · ")
    val name = category?.category?.name ?: stringResource(R.string.business_uncategorized)
    val headline = if (category?.category?.isDeleted == true) "$name ($deleted)" else name
    val receivedText = if (!showReceived) null else {
        if (destinationCurrency == null) unavailable else stringResource(
            R.string.business_received_amount,
            businessAmountText(received, destinationCurrency, primaryCurrency, hidden = hidden),
        )
    }
    BusinessTransactionRow(
        headline, supporting, timeText, amount, type,
        category?.icon?.categoryGlyph() ?: MaterialSymbols.MoreHoriz,
        modifier, onClick, currency, primaryCurrency, hidden, receivedText,
    )
}

@Composable
internal fun BusinessTransactionRow(
    categoryName: String,
    supportingText: String,
    timeText: String,
    amount: Long,
    type: TransactionType,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    currency: String = LocalLedgerCurrency.current,
    primaryCurrency: String = LocalLedgerCurrency.current,
    hidden: Boolean = false,
    amountSupportingText: String? = null,
) {
    val colors = LedgerStyle.colors
    val interaction = if (onClick == null) Modifier else Modifier.uiClickable(onClick = onClick)
    BoxWithConstraints(modifier.fillMaxWidth().then(interaction)) {
        val amountColumnWidth = maxWidth / 2
        Row(
            Modifier.fillMaxWidth().padding(
                horizontal = LedgerStyle.dimensions.transactionRowHorizontal,
                vertical = LedgerStyle.spacing.large,
            ),
            horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BusinessTypeIcon(icon, type, contentDescription = categoryName)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.tiny)) {
                Text(categoryName, style = LedgerStyle.typography.transactionTitle, color = colors.textPrimary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(supportingText, style = LedgerStyle.typography.label, color = colors.textSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(
                Modifier.widthIn(max = amountColumnWidth),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.tiny),
            ) {
                BusinessAmountText(amount, currency, type = type, primaryCurrency = primaryCurrency,
                    hidden = hidden, size = BusinessAmountSize.Transaction)
                if (amountSupportingText != null) Text(amountSupportingText,
                    style = LedgerStyle.typography.caption, color = colors.textSecondary)
                if (timeText.isNotBlank()) Text(timeText, style = LedgerStyle.typography.label, color = colors.textSecondary)
            }
        }
    }
}
