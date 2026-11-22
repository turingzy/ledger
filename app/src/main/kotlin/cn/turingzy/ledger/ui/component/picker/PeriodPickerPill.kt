package cn.turingzy.ledger.ui.component.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 周期/日期筛选胶囊按钮。
 *
 * 在账单、日历及统计页面共享该胶囊按钮的尺寸与排版样式，展示当前选中的时间周期文案，并支持点击调出时间选择弹窗。
 *
 * @param text 胶囊内展示的周期或日期文案。
 * @param onClick 点击胶囊时的回调函数。
 * @param modifier 应用于该组件的修饰符。
 * @param background 胶囊背景颜色，默认为 [LedgerStyle.colors.surface]。
 * @param description 无障碍语义描述文案。
 * @param trailingIcon 尾部图标（例如下拉小箭头），为空时不展示。
 */
@Composable
internal fun PeriodPickerPill(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = LedgerStyle.colors.surface,
    description: String? = null,
    trailingIcon: ImageVector? = null,
) {
    val foreground = LedgerStyle.colors.textPrimary
    Row(
        modifier
            .heightIn(min = 32.dp)
            .clip(LedgerStyle.shapes.pill)
            .background(background)
            .uiClickable(kind = UiInteractionKind.Navigation, onClick = onClick)
            .semantics { if (description != null) contentDescription = description }
            .padding(horizontal = LedgerStyle.spacing.large, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            style = LedgerStyle.typography.periodPill,
            color = foreground, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) Icon(trailingIcon, null, Modifier.size(17.dp), tint = foreground)
    }
}
