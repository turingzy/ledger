package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.LedgerCardAppearance
import cn.turingzy.ledger.ui.component.rememberBlurredBackdrop
import kotlin.math.roundToInt

@Composable
fun SummaryHeroBanner(
    cardMonthDisplay: String,
    balance: Long,
    expense: Long,
    income: Long,
    modifier: Modifier = Modifier,
    appearance: LedgerCardAppearance = LedgerCardAppearance(),
    background: ImageBitmap? = null,
    settingsEnabled: Boolean = true,
    onUploadBackground: () -> Unit = {},
    onTransparencyEnabledChange: (Boolean) -> Unit = {},
    onTransparencyChange: (Int) -> Unit = {},
) {
    var backVisible by rememberSaveable { mutableStateOf(false) }
    var previewPercent by remember(appearance.transparencyPercent) { mutableFloatStateOf(appearance.transparencyPercent.toFloat()) }
    val previewAppearance = appearance.copy(transparencyPercent = previewPercent.roundToInt())
    val defaultPainter = painterResource(R.drawable.bg_ledger_card)
    val backgroundPainter = remember(background, defaultPainter) { background?.let(::BitmapPainter) ?: defaultPainter }
    val blurredBackdrop = rememberBlurredBackdrop(R.drawable.bg_ledger_card, background)

    val colors = MaterialTheme.colorScheme

    Box(
        modifier.fillMaxWidth().height(152.dp)
            .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = colors.scrim.copy(alpha = 0.18f))
            .clip(RoundedCornerShape(20.dp))
            .border(0.5.dp, colors.onPrimary.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            backgroundPainter, null, Modifier.fillMaxSize(),
            alignment = BiasAlignment(0f, -0.2f), contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(colors.scrim.copy(alpha = 0.22f), colors.scrim.copy(alpha = 0.04f), colors.scrim.copy(alpha = 0.10f)),
                ),
            ),
        )
        Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter)) {
            SummaryCardGlass(previewAppearance, backgroundPainter, blurredBackdrop, showDivider = false)
        }
        AnimatedContent(
            targetState = backVisible,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn(tween(140, delayMillis = 40)) togetherWith fadeOut(tween(90))
            },
            label = "summaryCardSide",
        ) { showingBack ->
            if (showingBack) {
                SummaryCardSettings(
                    appearance = previewAppearance,
                    enabled = settingsEnabled,
                    onUploadBackground = onUploadBackground,
                    onTransparencyEnabledChange = onTransparencyEnabledChange,
                    onTransparencyChange = { previewPercent = it },
                    onTransparencyChangeFinished = { onTransparencyChange(previewPercent.roundToInt()) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                SummaryCardFront(
                    cardMonthDisplay = cardMonthDisplay,
                    balance = balance,
                    expense = expense,
                    income = income,
                )
            }
        }
        IconButton(
            onClick = { backVisible = !backVisible },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 18.dp, end = 12.dp).size(36.dp),
        ) {
            Icon(
                imageVector = if (backVisible) Icons.Default.Close else Icons.Default.MoreHoriz,
                contentDescription = if (backVisible) "关闭卡片设置" else "更多卡片设置",
                tint = colors.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryCardSettings(
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
        Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp)) {
            Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("透明", Modifier.width(64.dp), fontSize = 13.sp, color = colors.onPrimary)
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
                Text("透明度", Modifier.width(64.dp), fontSize = 13.sp, color = colors.onPrimary)
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
                                modifier = Modifier.requiredSize(20.dp).clip(CircleShape),
                                colors = sliderColors,
                                enabled = sliderEnabled,
                                thumbSize = DpSize(20.dp, 20.dp),
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
