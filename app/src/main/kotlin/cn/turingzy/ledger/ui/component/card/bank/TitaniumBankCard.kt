package cn.turingzy.ledger.ui.component.card.bank

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerShapes

/**
 * 渲染账户与卡面预览共用的卡面材质、封面图片及光影。
 *
 * 可点击的账户卡片由调用方提供无障碍描述。
 */
@Composable
internal fun TitaniumBankCard(
    template: CardTemplate?,
    modifier: Modifier = Modifier,
    motionEnabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val preset = template.titaniumPreset()
    val imageUri = template.titaniumImageUri()
    val tuning = DefaultTitaniumCardTuning
    val inspection = LocalInspectionMode.current
    val image = rememberTitaniumCardImage(imageUri, preset.base.toArgb())
    val dark = image?.luminance?.let { it < .20 } ?: preset.dark
    val (motion, gesture) = rememberTitaniumTilt(motionEnabled && !inspection, TitaniumDrag.AfterLongPress, tuning)
    Box(
        modifier.aspectRatio(BankCardAspectRatio)
            .then(gesture)
            .graphicsLayer {
                val (angle, dragStrength) = motion.value
                val maxAngle = lerp(tuning.gravityAngleDegrees, tuning.dragAngleDegrees, dragStrength)
                rotationX = -angle.y * maxAngle
                rotationY = angle.x * maxAngle
                cameraDistance = (size.width * (1.4f - .35f * dragStrength)).coerceAtLeast(1f)
                shape = LedgerShapes.bankCard
                clip = true
                shadowElevation = 6.dp.toPx()
                ambientShadowColor = Color.Black.copy(alpha = .36f)
                spotShadowColor = Color.Black.copy(alpha = .30f)
            }
            .titaniumEdge(dark)
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)),
    ) {
        if (image == null) {
            TitaniumMaterial(preset, motion, tuning, Modifier.matchParentSize())
            if (Build.VERSION.SDK_INT < 33 || inspection) TitaniumFallbackReflection(motion, tuning, Modifier.matchParentSize())
        } else {
            val reflection = if (Build.VERSION.SDK_INT >= 33 && !inspection) {
                Modifier.titaniumImageReflection(motion, tuning)
            } else Modifier
            Box(Modifier.matchParentSize().then(reflection).background(preset.base)) {
                Image(
                    bitmap = image.bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            if (Build.VERSION.SDK_INT < 33 || inspection) TitaniumFallbackReflection(motion, tuning, Modifier.matchParentSize())
        }
    }
}

/**
 * 为钛金属卡片边缘绘制微斜切的高光金属边框。
 *
 * @param dark 卡片背景是否偏暗，用于调整边框白反光与暗部阴影的透明度。
 */
private fun Modifier.titaniumEdge(dark: Boolean): Modifier = drawWithCache {
    val stroke = .75.dp.toPx()
    val inset = stroke / 2f
    val radius = LedgerShapes.bankCardRadius.toPx() - inset
    val edge = Brush.linearGradient(
        0f to Color.White.copy(alpha = if (dark) .42f else .86f),
        .48f to Color.White.copy(alpha = if (dark) .08f else .20f),
        .82f to Color.Black.copy(alpha = if (dark) .22f else .16f),
        1f to Color.White.copy(alpha = if (dark) .23f else .61f),
        end = Offset(size.width * .76f, size.height),
    )
    onDrawWithContent {
        drawContent()
        drawRoundRect(
            edge,
            topLeft = Offset(inset, inset),
            size = Size((size.width - stroke).coerceAtLeast(0f), (size.height - stroke).coerceAtLeast(0f)),
            cornerRadius = CornerRadius(radius),
            style = Stroke(stroke),
        )
    }
}
