package cn.turingzy.ledger.ui.feature.bill.sheet

import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.base.BottomSheetHeaderBase
import cn.turingzy.ledger.ui.component.base.DividerBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.ModalBottomSheetBase
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.ui.component.MaterialSymbols

/**
 * 记账与账单筛选中的账户选择底部抽屉组件。
 *
 * 以列表形式展示可用账户及其当前余额、货币代码，支持选中状态高亮标记、
 * 批量操作兼容数量提示、自定义头部及“全部账户”选项。
 *
 * @param title 抽屉标题文本。
 * @param accounts 候选账户列表 [AccountWithTemplate]。
 * @param selectedId 当前选中的账户 ID。
 * @param transactionType 当前交易类型（用于决定默认选中色）。
 * @param height 抽屉高度。
 * @param onDismiss 关闭抽屉回调。
 * @param onSelect 选定某个账户的回调（传入账户 ID）。
 * @param emptyMessage 账户列表为空时的提示文案。
 * @param optionHint 针对每个账户自定义显示的提示文本（例如批量操作时显示匹配数量）。
 * @param header 自定义抽屉头部组件；若为 null 则使用默认带关闭按钮的标题栏。
 * @param selectionColor 选中勾选标记的高亮颜色。
 * @param selectionCheckOnNameRow 勾选标记是否放置在账户名称同行右侧。
 * @param onSelectAll 可选的“全部账户”选项点击回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordAccountSheet(
    title: String,
    accounts: List<AccountWithTemplate>,
    selectedId: Long,
    transactionType: TransactionType,
    height: Dp,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
    emptyMessage: String? = null,
    optionHint: @Composable (Long) -> String? = { null },
    header: (@Composable () -> Unit)? = null,
    selectionColor: Color = LedgerStyle.colors.transaction(transactionType),
    selectionCheckOnNameRow: Boolean = false,
    onSelectAll: (() -> Unit)? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    ModalBottomSheetBase(
        onDismissRequest = onDismiss,
        contentHandlesInsets = true,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(Modifier.height(height).testTag("recordAccountDrawer").navigationBarsPadding()) {
            if (header != null) header()
            else BottomSheetHeaderBase(stringResource(R.string.ui_close_account_picker), onDismiss) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            DividerBase()
            if (accounts.isEmpty() && onSelectAll == null) {
                Text(emptyMessage ?: textResources.getString(cn.turingzy.ledger.R.string.ui_no_accounts_available_add_one_in_wallet), Modifier.padding(LedgerStyle.spacing.extraLarge), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                if (onSelectAll != null) item(key = "allAccounts") {
                    Row(
                        Modifier.fillMaxWidth().clickable(onClick = onSelectAll).padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
                    ) {
                        Icon(MaterialSymbols.AccountBalanceWallet, null, Modifier.size(LedgerStyle.dimensions.listIconContainer),
                            tint = LedgerStyle.colors.textSecondary)
                        Text(stringResource(R.string.bill_search_all_accounts), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        if (selectedId == 0L) Icon(
                            MaterialSymbols.Check, stringResource(R.string.ui_selected), tint = selectionColor,
                        )
                    }
                }
                items(accounts, key = { it.account.id }) { item ->
                    val selected = item.account.id == selectedId
                    val selectionCheck: @Composable () -> Unit = {
                        Icon(MaterialSymbols.Check, textResources.getString(cn.turingzy.ledger.R.string.ui_selected), tint = selectionColor)
                    }
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(item.account.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
                    ) {
                        cn.turingzy.ledger.ui.component.biz.AccountIconBiz(item.account)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.tiny)) {
                            if (selectionCheckOnNameRow) {
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 24.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
                                ) {
                                    Text(item.account.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    if (selected) selectionCheck()
                                }
                            } else {
                                Text(item.account.name, style = MaterialTheme.typography.bodyMedium)
                            }
                            Row(
                                Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
                            ) {
                                cn.turingzy.ledger.ui.component.biz.AmountTextBiz(
                                    item.account.currentBalance, item.account.currency,
                                    modifier = Modifier.weight(1f),
                                    textStyle = LedgerStyle.typography.bodySmall,
                                    contentColor = LedgerStyle.colors.textSecondary,
                                )
                                optionHint(item.account.id)?.let {
                                    Text(it, style = LedgerStyle.typography.label, color = LedgerStyle.colors.textSecondary, maxLines = 1)
                                }
                            }
                        }
                        if (selected && !selectionCheckOnNameRow) selectionCheck()
                    }
                }
            }
        }
    }
}
