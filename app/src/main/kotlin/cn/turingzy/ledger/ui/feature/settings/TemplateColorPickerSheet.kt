package cn.turingzy.ledger.ui.feature.settings

import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import cn.turingzy.ledger.ui.component.LedgerIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

private data class PickerColor(val hue: Float, val saturation: Float, val value: Float, val alpha: Float) {
    fun color() = Color.hsv(hue, saturation, value, alpha)
}

@Composable
internal fun TemplateColorPickerSheet(
    initialColor: String,
    onDismissRequest: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selection by remember { mutableStateOf(initialColor.parsePickerColor() ?: PickerColor(0f, 0f, 1f, 1f)) }
    var hex by remember { mutableStateOf(selection.color().hexString()) }
    val valid = hex.parsePickerColor() != null
    val colors = MaterialTheme.colorScheme

    fun update(value: PickerColor) {
        selection = value
        hex = value.color().hexString()
    }

    SettingsBottomSheet(
        title = "文字颜色",
        onDismissRequest = onDismissRequest,
        onBack = onDismissRequest,
        action = {
            IconButton(showPressFeedback = false, onClick = { onConfirm(selection.color().hexString()) }, enabled = valid) {
                Icon(MaterialSymbols.Check, "确认文字颜色", tint = colors.tertiary.copy(alpha = if (valid) 1f else 0.38f))
            }
        },
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SaturationValuePalette(selection, onChange = { saturation, value ->
                update(selection.copy(saturation = saturation, value = value))
            })
            Text("色相", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            ColorGradientSlider(
                value = selection.hue / 360f,
                stops = remember { (0..6).map { Color.hsv((it * 60f).coerceAtMost(359.999f), 1f, 1f) } },
                thumbColor = Color.hsv(selection.hue, 1f, 1f),
                label = "色相",
                onValueChange = { update(selection.copy(hue = (it * 360f).coerceAtMost(359.999f))) },
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("不透明度", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text("${(selection.alpha * 100).toInt()}%", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            ColorGradientSlider(
                value = selection.alpha,
                stops = listOf(selection.color().copy(alpha = 0f), selection.color().copy(alpha = 1f)),
                thumbColor = selection.color().copy(alpha = 1f),
                label = "颜色不透明度",
                onValueChange = { update(selection.copy(alpha = it)) },
            )
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant.copy(alpha = 0.45f)) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(32.dp).clip(CircleShape).background(selection.color()).border(0.5.dp, colors.outlineVariant, CircleShape))
                    Text("HEX", style = MaterialTheme.typography.bodyLarge)
                    BasicTextField(
                        value = hex,
                        onValueChange = { input ->
                            hex = input.uppercase().take(9)
                            hex.parsePickerColor()?.let { selection = it }
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.onSurface),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f).semantics { contentDescription = "HEX 颜色值" },
                    )
                }
            }
            Text(
                if (valid) "支持 #RRGGBB 或 #AARRGGBB" else "请输入有效的 6 位或 8 位 HEX 色值",
                style = MaterialTheme.typography.bodySmall,
                color = if (valid) colors.onSurfaceVariant else colors.error,
            )
        }
    }
}

@Composable
private fun SaturationValuePalette(selection: PickerColor, onChange: (Float, Float) -> Unit) {
    val update by rememberUpdatedState(onChange)
    Canvas(
        Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp))
            .semantics { contentDescription = "饱和度和明度色板，也可通过 HEX 输入精确颜色" }
            .pointerInput(Unit) {
                detectTapGestures { point ->
                    update((point.x / size.width).coerceIn(0f, 1f), (1f - point.y / size.height).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { point ->
                        update((point.x / size.width).coerceIn(0f, 1f), (1f - point.y / size.height).coerceIn(0f, 1f))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        update((change.position.x / size.width).coerceIn(0f, 1f), (1f - change.position.y / size.height).coerceIn(0f, 1f))
                    },
                )
            },
    ) {
        drawRect(Brush.horizontalGradient(listOf(Color.White, Color.hsv(selection.hue, 1f, 1f))))
        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val inset = 11.dp.toPx().coerceAtMost(minOf(size.width, size.height) / 2)
        val center = Offset(
            (selection.saturation * size.width).coerceIn(inset, size.width - inset),
            ((1f - selection.value) * size.height).coerceIn(inset, size.height - inset),
        )
        drawCircle(Color.Black, radius = 9.dp.toPx(), center = center, style = Stroke(3.dp.toPx()))
        drawCircle(Color.White, radius = 9.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorGradientSlider(
    value: Float,
    stops: List<Color>,
    thumbColor: Color,
    label: String,
    onValueChange: (Float) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        thumb = {
            Box(Modifier.size(22.dp).clip(CircleShape).background(thumbColor).border(2.dp, colors.surface, CircleShape))
        },
        track = {
            Box(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).background(colors.surfaceVariant).background(Brush.horizontalGradient(stops)))
        },
    )
}

private fun Color.hexString(): String {
    val argb = toArgb().toUInt().toString(16).uppercase().padStart(8, '0')
    return "#${if (argb.startsWith("FF")) argb.drop(2) else argb}"
}

private fun String.parsePickerColor(): PickerColor? {
    val hex = removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    if (hex.any { it.digitToIntOrNull(16) == null }) return null
    val value = hex.toLongOrNull(16) ?: return null
    val argb = (if (hex.length == 6) value or 0xFF000000L else value).toInt()
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(argb, hsv)
    return PickerColor(hsv[0], hsv[1], hsv[2], android.graphics.Color.alpha(argb) / 255f)
}
