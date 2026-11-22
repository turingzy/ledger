package cn.turingzy.ledger.ui.component.biz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle

private val TransactionNoteWhitespace = Regex("\\s+")

/**
 * 业务交易列表项组件。
 *
 * 使用交易分类与时间构建列表项，并为转账补充到账金额。软删除记录跳过绘制。
 *
 * @param amount 交易原始金额（以“分”为单位）。
 * @param currency 交易原始货币代码。
 * @param type 交易类型（支出、收入、转账）。
 * @param category 关联的分类及图标信息；为 null 时显示未分类。
 * @param timeText 格式化后的时间展示文本（如 "14:30" 或 "昨天"）。
 * @param note 可选的交易备注。
 * @param modifier 应用于交易项的修饰符。
 * @param primaryCurrency 当前主货币代码。
 * @param hidden 是否处于隐私模式（隐藏金额显示）。
 * @param isDeleted 当前交易记录是否已被标记删除（为 true 时不渲染）。
 * @param destinationCurrency 转账目标货币代码（仅转账类型有效）。
 * @param receivedAmount 转账实际到账金额（以“分”为单位）。
 * @param onClick 可选的点击事件回调。
 */
@Composable
fun TransactionItemBiz(
    amount: Long,
    currency: String,
    type: TransactionType,
    category: CategoryWithIcon?,
    timeText: String,
    modifier: Modifier = Modifier,
    primaryCurrency: String = LocalCurrencyBiz.current,
    hidden: Boolean = false,
    isDeleted: Boolean = false,
    destinationCurrency: String? = null,
    receivedAmount: Long? = null,
    note: String? = null,
    onClick: (() -> Unit)? = null,
) {
    if (isDeleted) return
    val deleted = stringResource(R.string.business_deleted)
    val received = receivedAmount ?: amount.takeIf { currency == destinationCurrency }
    val showReceived = type == TransactionType.TRANSFER && (destinationCurrency != currency || received != amount)
    val name = category?.category?.name ?: stringResource(R.string.business_uncategorized)
    val headline = if (category?.category?.isDeleted == true) "$name ($deleted)" else name
    val receivedText = if (!showReceived) null else {
        if (destinationCurrency == null) stringResource(R.string.business_unavailable_account) else stringResource(
            R.string.business_received_amount,
            amountTextBiz(received, destinationCurrency, primaryCurrency, hidden = hidden),
        )
    }
    TransactionRowBiz(
        headline, timeText, amount, type,
        category?.icon?.categoryGlyphBiz() ?: MaterialSymbols.MoreHoriz,
        modifier, onClick, currency, primaryCurrency, hidden, receivedText, note,
    )
}

/**
 * 交易列表项排版布局组件。
 *
 * 时间与分类名称左对齐，交易金额显示在右侧。
 *
 * @param categoryName 分类名称。
 * @param timeText 时间文本。
 * @param note 与时间并列的可选备注。
 * @param amount 交易金额（以“分”为单位）。
 * @param type 交易类型（支出、收入、转账）。
 * @param icon 分类或业务对应的矢量图标。
 * @param modifier 应用于行的修饰符。
 * @param onClick 可选的点击事件回调。
 * @param currency 交易货币代码。
 * @param primaryCurrency 主货币代码。
 * @param hidden 是否隐藏金额。
 * @param amountSupportingText 可选的金额下方辅助说明文本（如跨币种到账金额）。
 */
@Composable
internal fun TransactionRowBiz(
    categoryName: String,
    timeText: String,
    amount: Long,
    type: TransactionType,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    currency: String = LocalCurrencyBiz.current,
    primaryCurrency: String = LocalCurrencyBiz.current,
    hidden: Boolean = false,
    amountSupportingText: String? = null,
    note: String? = null,
) {
    val colors = LedgerStyle.colors
    val secondaryText = listOfNotNull(
        timeText.takeIf { it.isNotBlank() },
        note?.trim()?.replace(TransactionNoteWhitespace, " ")?.takeIf { it.isNotEmpty() },
    ).joinToString(" · ")
    val interaction = if (onClick == null) Modifier else Modifier.uiClickable(onClick = onClick)
    BoxWithConstraints(modifier.fillMaxWidth().then(interaction)) {
        val amountColumnWidth = maxWidth / 2
        Row(
            Modifier.fillMaxWidth().padding(
                horizontal = LedgerStyle.dimensions.transactionRowHorizontal,
                vertical = LedgerStyle.spacing.rowVertical,
            ),
            horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TypeIconBiz(icon, type, contentDescription = categoryName)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.hairline)) {
                    Text(categoryName, style = LedgerStyle.typography.transactionTitle, color = colors.textPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (secondaryText.isNotEmpty()) Text(secondaryText, style = LedgerStyle.typography.label,
                        color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(
                    Modifier.widthIn(max = amountColumnWidth),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.hairline),
                ) {
                    AmountTextBiz(amount, currency, type = type, primaryCurrency = primaryCurrency,
                        hidden = hidden, size = AmountSizeBiz.Transaction)
                    if (amountSupportingText != null) Text(amountSupportingText,
                        style = LedgerStyle.typography.caption, color = colors.textSecondary)
                }
            }
        }
    }
}
