package cn.turingzy.ledger.ui.component.input

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * 紧凑型滑块组件。
 *
 * 提供更小尺寸的滑块指示器（thumb）与轨道（track），常用于表单或设置面板中的数值调节。
 *
 * @param value 当前滑块数值。
 * @param onValueChange 数值改变时的回调函数。
 * @param modifier 应用于滑块的修饰符。
 * @param enabled 是否启用该滑块。
 * @param valueRange 可选数值范围，默认为 0f..1f。
 * @param onValueChangeFinished 滑动结束时的回调函数。
 * @param color 滑块与激活轨道的主题颜色，默认为当前主题的 primary 颜色。
 * @param thumbDiameter 滑块指示器的直径，默认为 16.dp。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompactSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: () -> Unit = {},
    color: Color = MaterialTheme.colorScheme.primary,
    thumbDiameter: Dp = 16.dp,
) {
    val interactions = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        thumbColor = color,
        activeTrackColor = color,
        inactiveTrackColor = color.copy(alpha = 0.24f),
        disabledThumbColor = color.copy(alpha = 0.38f),
        disabledActiveTrackColor = color.copy(alpha = 0.38f),
        disabledInactiveTrackColor = color.copy(alpha = 0.12f),
    )
    Slider(
        value = value, onValueChange = onValueChange, modifier = modifier.fillMaxWidth(), enabled = enabled,
        valueRange = valueRange, onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactions, colors = colors,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactions, modifier = Modifier.requiredSize(thumbDiameter).clip(CircleShape),
                colors = colors, enabled = enabled, thumbSize = DpSize(thumbDiameter, thumbDiameter),
            )
        },
        track = { state ->
            SliderDefaults.Track(
                sliderState = state, colors = colors, enabled = enabled, drawStopIndicator = null,
                thumbTrackGapSize = 0.dp, modifier = Modifier.height(4.dp),
            )
        },
    )
}
