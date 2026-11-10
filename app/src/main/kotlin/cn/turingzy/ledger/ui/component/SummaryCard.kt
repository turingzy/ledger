package cn.turingzy.ledger.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import kotlin.math.roundToInt
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.LedgerCardAppearance

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    appearance: LedgerCardAppearance = LedgerCardAppearance(),
    background: ImageBitmap? = null,
    @DrawableRes defaultBackgroundRes: Int = R.drawable.bg_ledger_card,
    settingsEnabled: Boolean = true,
    onUploadBackground: () -> Unit = {},
    onTransparencyChange: (Int) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    var settingsVisible by rememberSaveable { mutableStateOf(false) }
    var previewPercent by remember(appearance.transparencyPercent) {
        mutableFloatStateOf(appearance.transparencyPercent.toFloat())
    }
    val previewAppearance = appearance.copy(transparencyPercent = previewPercent.roundToInt())
    val defaultPainter = painterResource(defaultBackgroundRes)
    val backgroundPainter = remember(background, defaultPainter) {
        background?.let(::BitmapPainter) ?: defaultPainter
    }
    val blurredBackdrop = rememberBlurredBackdrop(defaultBackgroundRes, background)
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier.fillMaxWidth().height(152.dp)
            .shadow(12.dp, shape, spotColor = colors.scrim.copy(alpha = 0.18f))
            .clip(shape)
            .border(0.5.dp, colors.onPrimary.copy(alpha = 0.25f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            backgroundPainter, null, Modifier.fillMaxSize(),
            alignment = BiasAlignment(0f, -0.2f), contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        colors.scrim.copy(alpha = 0.22f),
                        colors.scrim.copy(alpha = 0.04f),
                        colors.scrim.copy(alpha = 0.10f),
                    ),
                ),
            ),
        )
        Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter)) {
            SummaryCardGlass(previewAppearance, backgroundPainter, blurredBackdrop, showDivider = false)
        }
        AnimatedContent(
            targetState = settingsVisible,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { fadeIn(tween(140, delayMillis = 40)) togetherWith fadeOut(tween(90)) },
            label = "summaryCardSide",
        ) { showingSettings ->
            if (showingSettings) {
                SummaryCardSettings(
                    appearance = previewAppearance,
                    enabled = settingsEnabled,
                    onUploadBackground = onUploadBackground,
                    onTransparencyChange = { previewPercent = it },
                    onTransparencyChangeFinished = { onTransparencyChange(previewPercent.roundToInt()) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize(), content = content)
            }
        }
        SummaryCardIconButton(
            imageVector = if (settingsVisible) MaterialSymbols.Close else MaterialSymbols.MoreHoriz,
            contentDescription = if (settingsVisible) "关闭卡片设置" else "更多卡片设置",
            onClick = { settingsVisible = !settingsVisible },
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 12.dp).size(36.dp),
        )
    }
}
