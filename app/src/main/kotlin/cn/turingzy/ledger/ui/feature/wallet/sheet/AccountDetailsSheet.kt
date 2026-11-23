package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.component.base.ModalBottomSheetBase
import cn.turingzy.ledger.ui.component.base.keepSheetAnchorDuringIme
import cn.turingzy.ledger.ui.component.base.DialogWindowBase
import cn.turingzy.ledger.ui.component.base.DialogActions
import cn.turingzy.ledger.ui.component.base.dialogContentPadding
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme
import kotlinx.coroutines.launch

/**
 * 账户详情与编辑底部弹窗。
 *
 * 包装 [AccountDetailsContent]，提供平滑收起动画及键盘弹起锚定处理。
 *
 * @param account 待查看或编辑的账户
 * @param onDismissRequest 关闭弹窗请求回调
 * @param onSave 保存账户修改回调
 * @param onDelete 删除账户回调，传回账户 ID
 * @param modifier 外部修饰符
 * @param templates 可用的卡面模板列表
 * @param sheetState 底部弹窗状态
 * @param assignedTemplate 当前账户关联的卡面模板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsSheet(
    account: Account,
    onDismissRequest: () -> Unit,
    onSave: (Account) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    assignedTemplate: CardTemplate? = null,
) {
    val scope = rememberCoroutineScope()
    fun dismiss() {
        scope.launch {
            sheetState.hide()
            if (!sheetState.isVisible) onDismissRequest()
        }
    }

    ModalBottomSheetBase(
        contentHandlesInsets = true,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier.keepSheetAnchorDuringIme(),
    ) {
        AccountDetailsContent(
            account = account,
            templates = templates,
            assignedTemplate = assignedTemplate,
            onDelete = { onDelete(account.id); dismiss() },
            onDismiss = ::dismiss,
            onSave = {
                onSave(it)
                dismiss()
            },
        )
    }
}

/**
 * 账户详情与编辑弹窗主体内容组件。
 *
 * 包含账户属性表单、修改保存、卡面替换入口以及删除确认弹窗交互。
 *
 * @param account 待查看或编辑的账户
 * @param onSave 保存账户修改回调
 * @param onDelete 确认删除账户回调
 * @param modifier 外部修饰符
 * @param templates 可用的卡面模板列表
 * @param onDismiss 关闭或取消回调
 * @param assignedTemplate 当前账户关联的卡面模板
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsContent(
    account: Account,
    onSave: (Account) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
    onDismiss: () -> Unit = {},
    assignedTemplate: CardTemplate? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var confirmDelete by rememberSaveable(account.id) { mutableStateOf(false) }
    AccountEditorContent(
        account = account,
        onDismiss = onDismiss,
        templates = templates,
        assignedTemplate = assignedTemplate,
        onSave = onSave,
        modifier = modifier,
        onDelete = { confirmDelete = true },
    )
    if (confirmDelete) {
        DialogWindowBase(onDismissRequest = { confirmDelete = false }) {
            SurfaceBase(
                shape = LedgerStyle.shapes.confirmationDialog,
                color = LedgerStyle.colors.overlay, contentColor = LedgerStyle.colors.textPrimary,
            ) {
                Column(
                    Modifier.dialogContentPadding(LedgerStyle.spacing.block),
                    verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraLarge),
                ) {
                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_delete_account_2), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "删除“${account.name}”后，该账户将不再显示，历史交易保留。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    DialogActions(
                        onCancel = { confirmDelete = false },
                        onConfirm = { confirmDelete = false; onDelete() },
                        confirmLabel = textResources.getString(cn.turingzy.ledger.R.string.bill_delete_action),
                        destructive = true,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountDetailsPreview() {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    LedgerTheme {
        AccountDetailsContent(
            account = Account(id = 1L, name = textResources.getString(cn.turingzy.ledger.R.string.ui_salary_account), type = AccountType.DEBIT, currentBalance = 123456L, cardNumber = "1234"),
            onSave = {},
            onDelete = {},
        )
    }
}
