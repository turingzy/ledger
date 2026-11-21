package cn.turingzy.ledger.ui.feature.settings.component

import cn.turingzy.ledger.ui.component.base.IconButtonContentBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.component.base.DialogWindowBase
import cn.turingzy.ledger.ui.component.base.DialogActions
import cn.turingzy.ledger.ui.component.base.dialogContentPadding
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import cn.turingzy.ledger.ui.component.input.CompactSlider
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.card.bank.asCardTextColor

private fun Color.asHex(): String {
    val hex = toArgb().toUInt().toString(16).uppercase().padStart(8, '0')
    return "#${if (hex.startsWith("FF")) hex.drop(2) else hex}"
}

@Composable
internal fun CardTextColorPicker(
    value: String,
    onChange: (String) -> Unit,
    automatic: Boolean = false,
) {
    val textResources = androidx.compose.ui.platform.LocalResources.current
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val presets = LedgerStyle.decorationColors.colorPickerPalette(colors)
    var showDialog by remember { mutableStateOf(false) }
    val current = value.asCardTextColor() ?: LedgerStyle.decorationColors.colorPickerDefault
    Column {
        Row(Modifier.fillMaxWidth().height(SettingsItemHeight), verticalAlignment = Alignment.CenterVertically) {
            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_text_color), Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
            IconButtonContentBase(onClick = { focusManager.clearFocus(); showDialog = true }) {
                Icon(MaterialSymbols.Palette, textResources.getString(cn.turingzy.ledger.R.string.ui_more_colors), Modifier.size(LedgerStyle.icons.default))
            }
        }
        Row(
            Modifier.fillMaxWidth().height(SettingsItemHeight).padding(horizontal = LedgerStyle.spacing.large),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            presets.forEach { preset ->
                val isSelected = !automatic && preset.toArgb() == current.toArgb()
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
    val textResources = androidx.compose.ui.platform.LocalResources.current
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
    DialogWindowBase(onDismissRequest = onDismiss) {
        SurfaceBase(modifier = Modifier.padding(vertical = LedgerStyle.spacing.large), shape = LedgerStyle.shapes.confirmationDialog, color = colors.surface) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).dialogContentPadding(LedgerStyle.spacing.extraLarge),
                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
            ) {
                Text(textResources.getString(cn.turingzy.ledger.R.string.ui_custom_color), style = MaterialTheme.typography.titleLarge)
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ColorSquare(hue, saturation, brightness, onSelect = { newHue, newSaturation ->
                        hue = newHue
                        saturation = newSaturation
                        updateColor()
                    })
                }
                Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(textResources.getString(cn.turingzy.ledger.R.string.ui_brightness), style = MaterialTheme.typography.bodyMedium)
                    CompactSlider(
                        value = brightness,
                        onValueChange = { brightness = it; updateColor() },
                        modifier = Modifier.weight(1f).padding(start = 12.dp).semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_brightness) },
                    )
                }
                SurfaceBase(shape = LedgerStyle.shapes.field, color = colors.surface) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
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
                            modifier = Modifier.weight(1f).semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_hex_color) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (parsed == null) colors.error else colors.onSurface),
                            cursorBrush = SolidColor(colors.onSurface),
                        )
                        Box(Modifier.size(LedgerStyle.icons.default).clip(CircleShape).background(parsed ?: initialColor).border(0.5.dp, colors.outlineVariant, CircleShape))
                    }
                }
                DialogActions(
                    onCancel = onDismiss,
                    onConfirm = { parsed?.let(onConfirm) },
                    confirmEnabled = parsed != null,
                )
            }
        }
    }
}

@Composable
private fun ColorSquare(hue: Float, saturation: Float, brightness: Float, onSelect: (Float, Float) -> Unit) {
    val textResources = androidx.compose.ui.platform.LocalResources.current
    val update by rememberUpdatedState(onSelect)
    val hueColors = remember(brightness) { (0..6).map { Color.hsv((it % 6) * 60f, 1f, brightness) } }
    Canvas(
        Modifier.fillMaxWidth().height(180.dp).clip(LedgerStyle.shapes.field)
            .semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_color_palette) }
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
        drawCircle(LedgerStyle.decorationColors.colorPickerOutline, 6.dp.toPx(), marker, style = Stroke(3.dp.toPx()))
        drawCircle(LedgerStyle.decorationColors.colorPickerDefault, 6.dp.toPx(), marker, style = Stroke(1.5.dp.toPx()))
    }
}
