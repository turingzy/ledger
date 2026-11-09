package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.LedgerCardAppearance

@Composable
internal fun SummaryCardGlass(
    appearance: LedgerCardAppearance,
    background: Painter,
    blurredBackdrop: ImageBitmap?,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val opacity = appearance.overlayOpacity
    Box(modifier.fillMaxSize().graphicsLayer { alpha = opacity }) {
        if (blurredBackdrop != null) {
            Image(
                bitmap = blurredBackdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, 0.7f),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(background, null, Modifier.fillMaxSize().blur(20.dp), contentScale = ContentScale.Crop)
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        colors.scrim.copy(alpha = 0.35f + 0.65f * opacity),
                        colors.scrim.copy(alpha = 0.55f + 0.45f * opacity),
                    ),
                ),
            ),
        )
        if (showDivider) {
            Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.onPrimary.copy(alpha = 0.32f)))
        }
    }
}
