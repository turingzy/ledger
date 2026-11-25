package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.base.BottomSheetHeaderBase
import cn.turingzy.ledger.ui.component.base.ButtonBase
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 账户操作底部弹窗的基础骨架布局。
 *
 * 表单独立滚动，新增账户的保存按钮固定在底部并避让键盘。
 *
 * @param title 弹窗标题文本
 * @param saveEnabled 保存按钮是否启用
 * @param onSave 点击保存回调
 * @param onDismiss 点击取消/关闭回调
 * @param modifier 外部修饰符
 * @param sheetHeight 展开时包含底部安全区的高度。
 * @param saveInFooter 是否将保存操作放在底部。
 * @param contentSpacing 内容项垂直间距
 * @param content 弹窗主体内容插槽
 */
@Composable
internal fun AccountSheetContent(
    title: String,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetHeight: Dp = LedgerStyle.dimensions.sheetMaxHeight,
    contentSpacing: Dp = 8.dp,
    saveInFooter: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val keyboardHeight = WindowInsets.ime.getBottom(density)
    val bottomInset = maxOf(keyboardHeight, WindowInsets.navigationBars.getBottom(density))
    Column(
        modifier.fillMaxWidth().height(sheetHeight)
            .padding(bottom = with(density) { bottomInset.toDp() }),
    ) {
        BottomSheetHeaderBase(
            stringResource(R.string.ui_close_named_sheet, title), onDismiss,
            modifier = Modifier.fillMaxWidth().height(44.dp).padding(horizontal = LedgerStyle.spacing.large),
            action = if (saveInFooter) null else ({ AccountSaveButton(saveEnabled, onSave) }),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = LedgerStyle.spacing.section).padding(top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(contentSpacing),
            content = content,
        )
        if (saveInFooter) {
            ButtonBase(
                label = stringResource(R.string.save),
                onClick = onSave,
                enabled = saveEnabled,
                shape = LedgerStyle.shapes.pill,
                modifier = Modifier.align(Alignment.End)
                    .padding(end = LedgerStyle.spacing.section, bottom = LedgerStyle.spacing.medium),
            )
        }
    }
}
