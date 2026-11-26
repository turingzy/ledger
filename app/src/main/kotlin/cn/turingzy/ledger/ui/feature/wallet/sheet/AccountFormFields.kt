package cn.turingzy.ledger.ui.feature.wallet.sheet

import cn.turingzy.ledger.ui.component.base.FormFieldFrameBase
import cn.turingzy.ledger.ui.component.base.IconButtonContentBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.MaterialSymbols

/**
 * 账户表单保存按钮（勾选图标）。
 *
 * @param enabled 是否可点击保存
 * @param onClick 点击保存回调
 * @param modifier 外部修饰符
 * @param contentDescription 无障碍辅助功能说明
 */
@Composable
internal fun AccountSaveButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = androidx.compose.ui.res.stringResource(cn.turingzy.ledger.R.string.ui_save_account),
) {
    val color = MaterialTheme.colorScheme.tertiary
    IconButtonContentBase(showPressFeedback = false, onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            MaterialSymbols.Check,
            contentDescription = contentDescription,
            modifier = Modifier.size(LedgerStyle.icons.default),
            tint = if (enabled) color else color.copy(alpha = 0.38f),
        )
    }
}

/**
 * 账户卡面样式选择入口字段。
 *
 * 显示当前卡面名称及右箭头，未启用时显示置灰状态及引导提示文本。
 *
 * @param value 当前选中的卡面名称或占位文案
 * @param onClick 点击打开卡面选择弹窗回调
 * @param modifier 外部修饰符
 * @param enabled 是否允许点击选择
 * @param disabledHint 禁用状态下的提示文案
 */
@Composable
internal fun AccountCardFaceField(
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    disabledHint: String = androidx.compose.ui.res.stringResource(cn.turingzy.ledger.R.string.ui_enter_an_account_name_first),
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    val contentColor = colors.onSurface.copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled)
    Column(modifier.fillMaxWidth()) {
        FormFieldFrameBase(textResources.getString(cn.turingzy.ledger.R.string.ui_card_design), enabled = enabled, onClick = if (enabled) onClick else null) {
            Text(value, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = contentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(MaterialSymbols.ChevronRight, null, Modifier.size(LedgerStyle.icons.inline), tint = contentColor)
        }
        if (!enabled) {
            Text(
                disabledHint,
                Modifier.padding(start = 12.dp, top = 4.dp),
                style = MaterialTheme.typography.labelSmall, color = LedgerStyle.colors.textSupporting,
            )
        }
    }
}
