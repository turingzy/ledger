package cn.turingzy.ledger.ui.component.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle

/** 单行周期控件的紧凑高度。 */
internal val PeriodPickerPillHeight = 32.dp

/**
 * 周期/日期筛选胶囊按钮。
 *
 * 在账单、日历及统计页面共享该胶囊按钮的尺寸与排版样式，展示当前选中的时间周期文案，并支持点击调出时间选择弹窗。
 *
 * @param text 胶囊内展示的日期文案。包含换行的区间以较小的字号分两行显示。
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
    val twoLines = '\n' in text
    Row(
        modifier
            .heightIn(min = PeriodPickerPillHeight)
            .clip(LedgerStyle.shapes.pill)
            .background(background)
            .uiClickable(kind = UiInteractionKind.Navigation, onClick = onClick)
            .semantics { if (description != null) contentDescription = description }
            .padding(horizontal = LedgerStyle.spacing.large, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val labelStyle = LedgerStyle.typography.periodPill
        Text(
            text,
            modifier = if (trailingIcon != null) Modifier.weight(1f, fill = false) else Modifier,
            style = if (twoLines) labelStyle.copy(
                fontSize = LedgerStyle.typography.label.fontSize,
                lineHeight = LedgerStyle.typography.label.lineHeight,
            ) else labelStyle,
            color = foreground,
            textAlign = if (twoLines) TextAlign.Center else null,
            maxLines = if (twoLines) 2 else 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (trailingIcon != null) Icon(trailingIcon, null, Modifier.size(LedgerStyle.icons.small), tint = foreground)
    }
}

/**
 * 周期前后切换的圆形导航箭头按钮。
 *
 * @param forward 是否为前进（下一周期），true 表示下一周期，false 表示上一周期。
 * @param onClick 点击事件回调。
 * @param modifier 修饰符。
 */
@Composable
internal fun PeriodNavigationArrow(
    forward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    Box(
        modifier.size(28.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .uiClickable(kind = UiInteractionKind.Navigation, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (forward) MaterialSymbols.ChevronRight else MaterialSymbols.ChevronLeft,
            if (forward) textResources.getString(cn.turingzy.ledger.R.string.ui_next_period) else textResources.getString(cn.turingzy.ledger.R.string.ui_previous_period),
            Modifier.size(LedgerStyle.icons.default), tint = MaterialTheme.colorScheme.primary,
        )
    }
}
