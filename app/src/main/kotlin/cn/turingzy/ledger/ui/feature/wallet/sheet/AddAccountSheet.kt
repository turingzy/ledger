package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.component.base.ModalBottomSheetBase
import cn.turingzy.ledger.ui.component.base.keepSheetAnchorDuringIme
import cn.turingzy.ledger.ui.component.base.rememberSheetDismiss
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme

/**
 * 新增账户底部弹窗。
 *
 * 包装 [AddAccountSheetContent]，提供弹窗展开/收起过渡动画及输入法弹起锚定适配。
 *
 * @param onDismissRequest 关闭弹窗请求回调
 * @param onConfirm 确认创建账户回调
 * @param modifier 外部修饰符
 * @param sheetState 底部弹窗状态
 * @param templates 可用的卡面模板列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    onDismissRequest: () -> Unit,
    onConfirm: (Account) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    templates: List<CardTemplate> = emptyList(),
) {
    val dismiss = rememberSheetDismiss(sheetState, onDismissRequest)

    ModalBottomSheetBase(
        contentHandlesInsets = true,
        onDismissRequest = dismiss::onHidden,
        sheetState = sheetState,
        modifier = modifier.keepSheetAnchorDuringIme(),
    ) {
        AddAccountSheetContent(
            onConfirm = { account ->
                if (sheetState.targetValue != SheetValue.Hidden) {
                    onConfirm(account)
                    dismiss.close()
                }
            },
            templates = templates,
            onDismiss = dismiss::close,
        )
    }
}

/**
 * 新增账户表单内容组件。
 *
 * 嵌入 [AccountEditorContent]，当保存成功后将账户回调给调用方。
 *
 * @param onConfirm 确认创建账户回调
 * @param modifier 外部修饰符
 * @param templates 可用的卡面模板列表
 * @param onDismiss 取消或关闭回调
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheetContent(
    onConfirm: (Account) -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
    onDismiss: () -> Unit = {},
) {
    AccountEditorContent(
        account = null,
        onDismiss = onDismiss,
        templates = templates,
        onSave = onConfirm,
        modifier = modifier,
    )
}


@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AddAccountSheetContentPreview() {
    LedgerTheme {
        AddAccountSheetContent(onConfirm = {})
    }
}
