package cn.turingzy.ledger.ui.component.input

import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 分段选择控制器（胶囊式分段切换器）。
 *
 * 在水平排列的分段标签之间切换，选中项带有平滑平移的高亮滑块背景。
 *
 * @param labels 各分段的文本标签列表。
 * @param selectedIndex 当前选中的分段索引。
 * @param onSelect 点击分段项时的回调，参数为选中的索引。
 * @param modifier 应用于控件的修饰符。
 * @param enabled 是否允许交互。
 * @param height 控件高度，默认为 36.dp，整体外形为高度一半的圆角胶囊形。
 * @param fontSize 标签文字大小，默认为 14.sp。
 * @param emphasizeSelection 选中时是否加粗文字（在未显式传入 [fontWeight] 时生效）。
 * @param containerColor 控件底层背景色。
 * @param selectedContainerColor 选中项指示器滑块的背景色。
 * @param fontWeight 统一指定的字重；若为空则根据选中状态和 [emphasizeSelection] 自动判断。
 */
@Composable
internal fun SegmentedControl(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 36.dp,
    fontSize: TextUnit = 14.sp,
    emphasizeSelection: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    selectedContainerColor: Color = MaterialTheme.colorScheme.surface,
    fontWeight: FontWeight? = null,
) {
    val shape = RoundedCornerShape(height / 2)
    BoxWithConstraints(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(containerColor)
            .padding(LedgerStyle.spacing.tiny),
    ) {
        val segmentWidth = maxWidth / labels.size
        val offset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex,
            animationSpec = tween(durationMillis = 220),
            label = "segmentSelectionOffset",
        )
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(shape)
                .background(selectedContainerColor),
        )
        Row(modifier = Modifier.fillMaxSize().selectableGroup()) {
            labels.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = selected,
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = { onSelect(index) },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        fontSize = fontSize,
                        lineHeight = fontSize,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        style = TextStyle(platformStyle = PlatformTextStyle(includeFontPadding = false)),
                        fontWeight = fontWeight ?: if (selected && emphasizeSelection) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
