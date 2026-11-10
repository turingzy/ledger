package cn.turingzy.ledger.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.LedgerCardAppearance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SummaryCardSettings(
    appearance: LedgerCardAppearance,
    enabled: Boolean,
    onUploadBackground: () -> Unit,
    onTransparencyEnabledChange: (Boolean) -> Unit,
    onTransparencyChange: (Float) -> Unit,
    onTransparencyChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val sliderEnabled = enabled && appearance.transparencyEnabled
    val sliderInteractions = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = colors.tertiary,
        activeTrackColor = colors.tertiary,
        inactiveTrackColor = colors.tertiary.copy(alpha = 0.24f),
        disabledThumbColor = colors.tertiary.copy(alpha = 0.38f),
        disabledActiveTrackColor = colors.tertiary.copy(alpha = 0.38f),
        disabledInactiveTrackColor = colors.tertiary.copy(alpha = 0.12f),
    )
    Box(modifier) {
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 6.dp)) {
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("透明", Modifier.width(52.dp), fontSize = 13.sp, color = colors.onPrimary)
                Box(Modifier.weight(1f).height(40.dp)) {
                    Switch(
                        checked = appearance.transparencyEnabled,
                        onCheckedChange = onTransparencyEnabledChange,
                        enabled = enabled,
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = colors.tertiary,
                            checkedThumbColor = colors.onPrimary,
                            checkedBorderColor = colors.tertiary,
                        ),
                        modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp)
                            .scale(0.72f)
                            .semantics { contentDescription = "透明开关" },
                    )
                    IconButton(
                        onClick = onUploadBackground,
                        enabled = enabled,
                        modifier = Modifier.align(Alignment.CenterEnd).padding(end = 38.dp).size(36.dp),
                    ) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "更换卡片背景",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("透明度", Modifier.width(52.dp), fontSize = 13.sp, color = colors.onPrimary)
                BoxWithConstraints(Modifier.weight(1f).height(40.dp)) {
                    Slider(
                        value = appearance.transparencyPercent.toFloat(),
                        onValueChange = onTransparencyChange,
                        onValueChangeFinished = onTransparencyChangeFinished,
                        enabled = sliderEnabled,
                        valueRange = 0f..100f,
                        interactionSource = sliderInteractions,
                        colors = sliderColors,
                        thumb = {
                            SliderDefaults.Thumb(
                                interactionSource = sliderInteractions,
                                modifier = Modifier.requiredSize(16.dp).clip(CircleShape),
                                colors = sliderColors,
                                enabled = sliderEnabled,
                                thumbSize = DpSize(16.dp, 16.dp),
                            )
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                colors = sliderColors,
                                enabled = sliderEnabled,
                                drawStopIndicator = null,
                                thumbTrackGapSize = 0.dp,
                                modifier = Modifier.height(4.dp),
                            )
                        },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "透明度" },
                    )
                    Text(
                        "${appearance.transparencyPercent}%",
                        modifier = Modifier.offset(
                            x = (maxWidth - 40.dp).coerceAtLeast(0.dp) * (appearance.transparencyPercent / 100f),
                            y = (-2).dp,
                        ),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimary,
                    )
                }
            }
        }
    }
}
