package cn.turingzy.ledger.ui.component.biz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.vector.ImageVector
import cn.turingzy.ledger.ui.component.MaterialSymbols
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
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.interaction.uiClickable

/**
 * 业务交易列表项组件。
 *
 * 封装单条交易记录的展示逻辑，包括分类图标与标题、账户或转账流向、备注、删除标记状态、跨币种实际到账金额提示，
 * 以及右侧交易金额和时间信息。已标记软删除的记录将直接跳过绘制。
 *
 * @param amount 交易原始金额（以“分”为单位）。
 * @param currency 交易原始货币代码。
 * @param type 交易类型（支出、收入、转账）。
 * @param category 关联的分类及图标信息；为 null 时显示未分类。
 * @param accountName 来源账户名称。
 * @param timeText 格式化后的时间展示文本（如 "14:30" 或 "昨天"）。
 * @param modifier 应用于交易项的修饰符。
 * @param note 交易备注文本。
 * @param primaryCurrency 当前主货币代码。
 * @param hidden 是否处于隐私模式（隐藏金额显示）。
 * @param isDeleted 当前交易记录是否已被标记删除（为 true 时不渲染）。
 * @param accountDeleted 来源账户是否已被归档或删除。
 * @param destinationName 转账目标账户名称（仅转账类型有效）。
 * @param destinationCurrency 转账目标货币代码（仅转账类型有效）。
 * @param destinationDeleted 目标账户是否已被归档或删除。
 * @param receivedAmount 转账实际到账金额（以“分”为单位）。
 * @param onClick 可选的点击事件回调。
 */
@Composable
fun TransactionItemBiz(
    amount: Long,
    currency: String,
    type: TransactionType,
    category: CategoryWithIcon?,
    accountName: String,
    timeText: String,
    modifier: Modifier = Modifier,
    note: String? = null,
    primaryCurrency: String = LocalCurrencyBiz.current,
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
            amountTextBiz(received, destinationCurrency, primaryCurrency, hidden = hidden),
        )
    }
    TransactionRowBiz(
        headline, supporting, timeText, amount, type,
        category?.icon?.categoryGlyphBiz() ?: MaterialSymbols.MoreHoriz,
        modifier, onClick, currency, primaryCurrency, hidden, receivedText,
    )
}

/**
 * 交易列表项排版布局组件。
 *
 * 组织左侧分类徽标、中间分类名称与次要信息（账户/备注）、右侧金额与时间文本的水平结构。
 *
 * @param categoryName 分类名称。
 * @param supportingText 辅助说明文本（如账户信息或备注）。
 * @param timeText 时间文本。
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
    supportingText: String,
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
            TypeIconBiz(icon, type, contentDescription = categoryName)
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
                AmountTextBiz(amount, currency, type = type, primaryCurrency = primaryCurrency,
                    hidden = hidden, size = AmountSizeBiz.Transaction)
                if (amountSupportingText != null) Text(amountSupportingText,
                    style = LedgerStyle.typography.caption, color = colors.textSecondary)
                if (timeText.isNotBlank()) Text(timeText, style = LedgerStyle.typography.label, color = colors.textSecondary)
            }
        }
    }
}
