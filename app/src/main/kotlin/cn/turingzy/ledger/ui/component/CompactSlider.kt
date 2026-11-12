package cn.turingzy.ledger.ui.component

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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

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
                interactionSource = interactions, modifier = Modifier.requiredSize(16.dp).clip(CircleShape),
                colors = colors, enabled = enabled, thumbSize = DpSize(16.dp, 16.dp),
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
