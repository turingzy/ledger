package cn.turingzy.ledger.ui.component.card.bank

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import cn.turingzy.ledger.ui.theme.LedgerShapes

@Composable
internal fun TitaniumBankCard(
    name: String,
    modifier: Modifier = Modifier,
    preset: TitaniumPreset = TitaniumPreset.IridescentLight,
    typeLabel: String? = null,
    tailNumber: String? = null,
    imageUri: Uri? = null,
    textColor: Color? = null,
    motionEnabled: Boolean = true,
    drag: TitaniumDrag = TitaniumDrag.Immediate,
    tuning: TitaniumCardTuning = DefaultTitaniumCardTuning,
    onClick: (() -> Unit)? = null,
) {
    TitaniumCardSurface(
        preset = preset,
        imageUri = imageUri,
        modifier = modifier,
        textColor = textColor,
        motionEnabled = motionEnabled,
        drag = drag,
        tuning = tuning,
        onClick = onClick,
    ) { ink ->
        val style = MaterialTheme.typography.bodyMedium.copy(
            color = ink,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
        )
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 20.dp, end = 46.dp, top = 17.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                name,
                Modifier.weight(1f, fill = false),
                style = style.copy(fontSize = 17.sp, lineHeight = 21.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!typeLabel.isNullOrBlank()) {
                Text(
                    typeLabel,
                    Modifier.border(.75.dp, ink.copy(alpha = .46f), CircleShape)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    style = style.copy(fontSize = 11.sp, lineHeight = 14.sp),
                    maxLines = 1,
                )
            }
        }

        val tail = tailNumber.orEmpty().filter { it in '0'..'9' }.takeLast(4)
        if (tail.isNotEmpty()) {
            Text(
                "•••• $tail",
                Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 19.dp),
                style = style.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontFeatureSettings = "tnum",
                ),
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun TitaniumCardSurface(
    preset: TitaniumPreset,
    imageUri: Uri?,
    modifier: Modifier = Modifier,
    textColor: Color? = null,
    motionEnabled: Boolean = false,
    drag: TitaniumDrag = TitaniumDrag.Disabled,
    tuning: TitaniumCardTuning = DefaultTitaniumCardTuning,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(Color) -> Unit,
) {
    val inspection = LocalInspectionMode.current
    val image = rememberTitaniumCardImage(imageUri, preset.base.toArgb())
    val dark = image?.luminance?.let { it < .20 } ?: preset.dark
    val ink = textColor ?: if (dark) Color(0xFFF0EDE7) else Color(0xFF28262A)
    val (motion, gesture) = rememberTitaniumTilt(motionEnabled && !inspection, drag, tuning)
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
            // RenderEffect samples the image layer alone, leaving the etched lettering unlit.
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
        content(ink)
        EtchedContactlessWaves(ink, Modifier.align(Alignment.TopEnd).padding(top = 17.dp, end = 20.dp))
    }
}

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

@Composable
private fun EtchedContactlessWaves(ink: Color, modifier: Modifier) {
    Canvas(modifier.size(width = 18.dp, height = 20.dp)) {
        val center = Offset(2.dp.toPx(), size.height / 2)
        for (wave in 1..4) {
            val radius = (wave * 3.6f).dp.toPx()
            drawArc(
                color = ink.copy(alpha = .85f),
                startAngle = -37f,
                sweepAngle = 74f,
                useCenter = false,
                topLeft = center - Offset(radius, radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(1.1.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}
