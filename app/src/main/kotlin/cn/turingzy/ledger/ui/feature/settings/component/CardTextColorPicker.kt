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

/**
 * 将 [Color] 对象转换为十六进制颜色字符串（如 "#FFFFFF" 或 "#AARRGGBB"）。
 */
private fun Color.asHex(): String {
    val hex = toArgb().toUInt().toString(16).uppercase().padStart(8, '0')
    return "#${if (hex.startsWith("FF")) hex.drop(2) else hex}"
}

/**
 * 卡片文字颜色选择器组件。
 *
 * 提供预设色板快速点击选择，并支持打开高级颜色调节对话框进行精细化配色。
 *
 * @param value 当前选中的颜色十六进制字符串
 * @param onChange 颜色发生改变时的回调
 * @param automatic 是否启用自适应/自动配色；若为 true 则预设色板不显示选中高亮
 */
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

/**
 * 高级自定义颜色选择对话框。
 *
 * 包含色盘二维取色器、亮度调节滑块以及支持 Hex 字符串直接编辑的文本框。
 *
 * @param initialColor 打开对话框时的初始颜色
 * @param onDismiss 对话框请求关闭或取消时的回调
 * @param onConfirm 确认选中新颜色时的回调
 */
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
        SurfaceBase(
            modifier = Modifier.padding(vertical = LedgerStyle.spacing.large),
            shape = LedgerStyle.shapes.confirmationDialog,
            color = LedgerStyle.colors.overlay, contentColor = LedgerStyle.colors.textPrimary,
        ) {
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
                SurfaceBase(shape = LedgerStyle.shapes.field, color = LedgerStyle.colors.overlayFieldBackground) {
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

/**
 * 基于 Canvas 绘制的二维色相-饱和度拾色板。
 *
 * 水平方向代表色相（Hue: 0°~360°），垂直方向代表饱和度（Saturation: 0~1），
 * 支持点击与拖拽手势交互并实时绘制指示圆环。
 *
 * @param hue 当前色相值 (0f..360f)
 * @param saturation 当前饱和度值 (0f..1f)
 * @param brightness 当前明度值 (0f..1f)
 * @param onSelect 选取的色相与饱和度变更回调 `(hue, saturation) -> Unit`
 */
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
