package cn.turingzy.ledger.ui.component.card.bank

import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

/**
 * 钛金属质感银行卡组件。
 *
 * 结合 AGSL 物理着色器或图形渐变模拟金属拉丝、各向异性反光以及重力感应倾斜效果。
 * 卡片上展示银行/账户名称与尾号信息。
 *
 * @param name 卡片/账户名称。
 * @param modifier 修饰符。
 * @param preset 钛金属材质预设，定义底色、彩虹色谱及金属拉丝纹理参数。
 * @param tailNumber 银行卡号，自动提取后四位数字展示在卡片右下角。
 * @param imageUri 自定义背景图片 URI，若提供则优先作为卡面背景并叠加反光。
 * @param textColor 卡面文字颜色，为 `null` 时根据背景明暗度自动计算高对比度颜色。
 * @param motionEnabled 是否启用物理感应倾斜效果（重力陀螺仪及手势拖拽）。
 * @param drag 手势拖拽交互模式。
 * @param tuning 钛金属材质与动效物理调节参数。
 * @param onClick 卡片点击回调。
 */
@Composable
internal fun TitaniumBankCard(
    name: String,
    modifier: Modifier = Modifier,
    preset: TitaniumPreset = TitaniumPreset.IridescentLight,
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
        Text(
            name,
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 17.dp),
            style = style.copy(fontSize = 17.sp, lineHeight = 21.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

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

/**
 * 钛金属质感卡片容器表面（Surface）。
 *
 * 封装了卡片的比例裁剪、3D 倾斜阴影变换、AGSL/降级反光材质渲染以及边缘高光描边，
 * 并将根据卡面亮度计算得到的自适应前景色 `ink` 传递给子内容构建块。
 *
 * @param preset 钛金属材质预设。
 * @param imageUri 自定义背景图片 URI。
 * @param modifier 修饰符。
 * @param textColor 强制指定的文字前景色；若为 `null` 则根据背景自适应计算。
 * @param motionEnabled 是否启用物理感应倾斜效果。
 * @param drag 手势拖拽交互模式。
 * @param tuning 钛金属材质与动效物理调节参数。
 * @param onClick 点击事件回调。
 * @param content 卡片内部内容布局插槽，接收计算出的合适文字颜色。
 */
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
