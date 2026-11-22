package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.component.base.ModalBottomSheetBase
import cn.turingzy.ledger.ui.component.base.keepSheetAnchorDuringIme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme
import kotlinx.coroutines.launch

/**
 * 新增账户底部弹窗。
 *
 * 包装 [AddAccountSheetContent]，提供弹窗展开/收起过渡动画及输入法弹起锚定适配。
 *
 * @param onDismissRequest 关闭弹窗请求回调
 * @param onConfirm 确认创建账户回调，传回名称、类型、期初余额、卡号、是否计入总资产、币种与卡面模板 ID
 * @param modifier 外部修饰符
 * @param sheetState 底部弹窗状态
 * @param templates 可用的卡面模板列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    onDismissRequest: () -> Unit,
    onConfirm: (
        name: String,
        type: AccountType,
        initialBalance: Long,
        cardNumber: String?,
        includeInTotal: Boolean,
        currency: String,
        templateId: Long?,
    ) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    templates: List<CardTemplate> = emptyList(),
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
        AddAccountSheetContent(
            onConfirm = { name, type, balance, cardNumber, includeInTotal, currency, templateId ->
                onConfirm(name, type, balance, cardNumber, includeInTotal, currency, templateId)
                dismiss()
            },
            templates = templates,
            onDismiss = ::dismiss,
        )
    }
}

/**
 * 新增账户表单内容组件。
 *
 * 嵌入 [AccountEditorContent]，当保存成功后将属性拆包回调给调用方。
 *
 * @param onConfirm 确认创建回调，传回账户各字段属性
 * @param modifier 外部修饰符
 * @param templates 可用的卡面模板列表
 * @param onDismiss 取消或关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheetContent(
    onConfirm: (
        name: String,
        type: AccountType,
        initialBalance: Long,
        cardNumber: String?,
        includeInTotal: Boolean,
        currency: String,
        templateId: Long?,
    ) -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
    onDismiss: () -> Unit = {},
) {
    AccountEditorContent(
        account = null,
        onDismiss = onDismiss,
        templates = templates,
        onSave = { value ->
            onConfirm(
                value.name, value.type, value.initialBalance, value.cardNumber,
                value.includeInTotal, value.currency, value.templateId,
            )
        },
        modifier = modifier,
    )
}


@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AddAccountSheetContentPreview() {
    LedgerTheme {
        AddAccountSheetContent(onConfirm = { _, _, _, _, _, _, _ -> })
    }
}
