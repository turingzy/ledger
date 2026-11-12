package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.CompactSlider
import cn.turingzy.ledger.ui.component.LedgerIconButton
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.theme.IosOrange
import cn.turingzy.ledger.ui.theme.IosPurple
import cn.turingzy.ledger.ui.theme.IosYellow
import cn.turingzy.ledger.ui.theme.IosTeal
import cn.turingzy.ledger.ui.theme.IosLabel
import cn.turingzy.ledger.ui.theme.SurfaceLight

internal fun String.asCardTextColor(): Color? {
    val hex = removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    if (hex.any { it.digitToIntOrNull(16) == null }) return null
    val value = hex.toLongOrNull(16) ?: return null
    return Color((if (hex.length == 6) value or 0xFF000000L else value).toInt())
}

private fun Color.asHex(): String {
    val hex = toArgb().toUInt().toString(16).uppercase().padStart(8, '0')
    return "#${if (hex.startsWith("FF")) hex.drop(2) else hex}"
}

@Composable
internal fun CardTextColorPicker(value: String, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val presets = listOf(SurfaceLight, IosLabel, colors.primary, colors.tertiary, colors.error, IosOrange, IosPurple, IosYellow, IosTeal)
    var showDialog by remember { mutableStateOf(false) }
    val current = value.asCardTextColor() ?: SurfaceLight
    Column {
        Row(Modifier.fillMaxWidth().height(SettingsItemHeight), verticalAlignment = Alignment.CenterVertically) {
            Text("文字颜色", Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
            LedgerIconButton(onClick = { focusManager.clearFocus(); showDialog = true }) {
                Icon(MaterialSymbols.Palette, "更多颜色", Modifier.size(24.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth().height(SettingsItemHeight).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            presets.forEach { preset ->
                val isSelected = preset.toArgb() == current.toArgb()
                Box(
                    Modifier.size(28.dp).border(if (isSelected) 2.dp else 0.5.dp, if (isSelected) colors.primary else colors.outlineVariant, CircleShape)
                        .padding(3.dp).clip(CircleShape).background(preset)
                        .clickable(role = Role.Button) { onChange(preset.asHex()) }
                        .semantics { contentDescription = "文字颜色 ${preset.asHex()}"; selected = isSelected },
                )
            }
        }
    }
    if (showDialog) CardTextColorDialog(
        initialColor = current,
        onDismiss = { showDialog = false },
        onConfirm = { onChange(it.asHex()); showDialog = false },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardTextColorDialog(initialColor: Color, onDismiss: () -> Unit, onConfirm: (Color) -> Unit) {
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember { mutableStateOf(initialHsv[0]) }
    var saturation by remember { mutableStateOf(initialHsv[1]) }
    var brightness by remember { mutableStateOf(initialHsv[2]) }
    var alpha by remember { mutableStateOf(initialColor.alpha) }
    var hex by remember { mutableStateOf(initialColor.asHex()) }
    val parsed = hex.asCardTextColor()
    val colors = MaterialTheme.colorScheme
    fun updateColor() { hex = Color.hsv(hue, saturation, brightness, alpha).asHex() }
    BasicAlertDialog(onDismissRequest = onDismiss) {
        Surface(modifier = Modifier.padding(vertical = 12.dp), shape = RoundedCornerShape(28.dp), color = colors.background) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("自定义颜色", style = MaterialTheme.typography.titleLarge)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ColorSquare(hue, saturation, brightness, onSelect = { newHue, newSaturation ->
                        hue = newHue
                        saturation = newSaturation
                        updateColor()
                    })
                }
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("明暗", style = MaterialTheme.typography.bodyMedium)
                    CompactSlider(
                        value = brightness,
                        onValueChange = { brightness = it; updateColor() },
                        modifier = Modifier.weight(1f).padding(start = 12.dp).semantics { contentDescription = "明暗" },
                    )
                }
                Surface(shape = RoundedCornerShape(12.dp), color = colors.surface) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = hex,
                            onValueChange = { input ->
                                hex = input.uppercase().take(9)
                                hex.asCardTextColor()?.let { color ->
                                    val hsv = FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
                                    hue = hsv[0]; saturation = hsv[1]; brightness = hsv[2]; alpha = color.alpha
                                }
                            },
                            modifier = Modifier.weight(1f).semantics { contentDescription = "HEX 颜色值" },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (parsed == null) colors.error else colors.onSurface),
                            cursorBrush = SolidColor(colors.onSurface),
                        )
                        Box(Modifier.size(24.dp).clip(CircleShape).background(parsed ?: initialColor).border(0.5.dp, colors.outlineVariant, CircleShape))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    LedgerIconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) { Icon(MaterialSymbols.Close, "取消颜色选择") }
                    LedgerIconButton(modifier = Modifier.size(40.dp), enabled = parsed != null, onClick = { parsed?.let(onConfirm) }) {
                        Icon(MaterialSymbols.Check, "确认文字颜色", tint = colors.tertiary.copy(alpha = if (parsed == null) 0.38f else 1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSquare(hue: Float, saturation: Float, brightness: Float, onSelect: (Float, Float) -> Unit) {
    val update by rememberUpdatedState(onSelect)
    val hueColors = remember(brightness) { (0..6).map { Color.hsv((it % 6) * 60f, 1f, brightness) } }
    Canvas(
        Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp))
            .semantics { contentDescription = "方形色板" }
            .pointerInput(Unit) {
                detectTapGestures { point ->
                    update((point.x / size.width).coerceIn(0f, 1f) * 359.99f, (point.y / size.height).coerceIn(0f, 1f))
                }
            }.pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    update((change.position.x / size.width).coerceIn(0f, 1f) * 359.99f, (change.position.y / size.height).coerceIn(0f, 1f))
                }
            },
    ) {
        drawRect(Brush.horizontalGradient(hueColors))
        drawRect(Brush.verticalGradient(listOf(Color.hsv(0f, 0f, brightness), Color.Transparent)))
        val inset = 8.dp.toPx()
        val marker = Offset(
            (hue / 360f * size.width).coerceIn(inset, size.width - inset),
            (saturation * size.height).coerceIn(inset, size.height - inset),
        )
        drawCircle(IosLabel, 6.dp.toPx(), marker, style = Stroke(3.dp.toPx()))
        drawCircle(SurfaceLight, 6.dp.toPx(), marker, style = Stroke(1.5.dp.toPx()))
    }
}
