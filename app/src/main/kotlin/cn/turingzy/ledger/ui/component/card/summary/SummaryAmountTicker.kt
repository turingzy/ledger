package cn.turingzy.ledger.ui.component.card.summary

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.util.toFormattedAmount

/** An open transaction sheet defers visible amount changes until the sheet closes. */
internal val LocalSummaryAmountsObscured = staticCompositionLocalOf { false }

/** Rolls changed digits while retaining ordinary text layout and a single accessible amount. */
@Composable
internal fun SummaryAmountTicker(
    amount: Long,
    currencySymbol: String,
    hidden: Boolean,
    animate: Boolean,
    style: TextStyle,
) {
    val amountStyle = style.copy(fontFeatureSettings = "tnum")
    val autoSize = remember(style.fontSize) {
        TextAutoSize.StepBased(minFontSize = 1.sp, maxFontSize = style.fontSize)
    }
    if (hidden) {
        Text("****", style = amountStyle, maxLines = 1, softWrap = false, autoSize = autoSize)
        return
    }

    val obscured = LocalSummaryAmountsObscured.current
    var visible by remember { mutableStateOf(false) }
    val canAnimate = animate && visible
    var shownAmount by remember { mutableLongStateOf(amount) }
    var previousAmount by remember { mutableLongStateOf(amount) }
    val latestAmount by rememberUpdatedState(amount)
    val progress = remember { Animatable(1f) }

    LaunchedEffect(canAnimate, obscured) {
        progress.snapTo(1f)
        if (canAnimate && obscured) return@LaunchedEffect
        snapshotFlow { latestAmount }.collect { nextAmount ->
            if (!canAnimate) {
                shownAmount = nextAmount
                previousAmount = nextAmount
            } else if (nextAmount != shownAmount) {
                previousAmount = shownAmount
                shownAmount = nextAmount
                progress.snapTo(0f)
                progress.animateTo(1f, tween(360))
            }
        }
    }

    val shown = if (canAnimate) shownAmount else amount
    val text = remember(shown, currencySymbol) {
        shown.toFormattedAmount(showSymbol = true, symbol = currencySymbol, useGrouping = true)
    }
    val previousText = remember(previousAmount, currencySymbol) {
        previousAmount.toFormattedAmount(showSymbol = true, symbol = currencySymbol, useGrouping = true)
    }
    val textMeasurer = rememberTextMeasurer()
    var textLayout by remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        style = amountStyle,
        maxLines = 1,
        softWrap = false,
        autoSize = autoSize,
        onTextLayout = { textLayout = it },
        modifier = Modifier.onVisibilityChanged(minFractionVisible = 0.5f) { visible = it }.drawWithCache {
            val target = textLayout?.takeIf { it.layoutInput.text.text == text }
            if (target == null || !canAnimate || obscured || text == previousText) {
                onDrawBehind {}
            } else {
                val source = textMeasurer.measure(
                    previousText,
                    style = target.layoutInput.style,
                    maxLines = 1,
                    softWrap = false,
                )
                val digits = text.mapIndexedNotNull { index, character ->
                    val oldIndex = index + previousText.length - text.length
                    val oldCharacter = previousText.getOrNull(oldIndex)
                    if (!character.isDigit() || character == oldCharacter) return@mapIndexedNotNull null
                    val glyph = target.getBoundingBox(index)
                    val line = target.getLineForOffset(index)
                    val bounds = Rect(glyph.left, target.getLineTop(line), glyph.right, target.getLineBottom(line))
                    val oldGlyph = oldCharacter?.takeIf { it.isDigit() }?.let { source.getBoundingBox(oldIndex) }
                    val sourceOffset = if (oldGlyph != null) {
                        val oldLine = source.getLineForOffset(oldIndex)
                        Offset(bounds.center.x - oldGlyph.center.x, bounds.top - source.getLineTop(oldLine))
                    } else {
                        Offset.Zero
                    }
                    DigitRoll(bounds, oldGlyph, sourceOffset)
                }
                val movingArea = Path().apply { digits.forEach { addRect(it.bounds) } }
                val direction = if (shown > previousAmount) -1f else 1f

                onDrawWithContent {
                    val fraction = progress.value
                    if (fraction >= 1f || digits.isEmpty()) {
                        drawContent()
                    } else {
                        clipPath(movingArea, ClipOp.Difference) { this@onDrawWithContent.drawContent() }
                        digits.forEach { (bounds, sourceGlyph, sourceOffset) ->
                            val distance = bounds.height * direction
                            clipRect(bounds.left, bounds.top, bounds.right, bounds.bottom) {
                                if (sourceGlyph != null) {
                                    val oldOffset = fraction * distance
                                    clipRect(
                                        left = sourceGlyph.left + sourceOffset.x,
                                        top = bounds.top + oldOffset,
                                        right = sourceGlyph.right + sourceOffset.x,
                                        bottom = bounds.bottom + oldOffset,
                                    ) {
                                        drawText(source, topLeft = sourceOffset + Offset(0f, oldOffset))
                                    }
                                }
                                val newOffset = (fraction - 1f) * distance
                                clipRect(top = bounds.top + newOffset, bottom = bounds.bottom + newOffset) {
                                    drawText(target, topLeft = Offset(0f, newOffset))
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

/** The target cell and source glyph bounds, which may have different widths. */
private data class DigitRoll(val bounds: Rect, val sourceGlyph: Rect?, val sourceOffset: Offset)
