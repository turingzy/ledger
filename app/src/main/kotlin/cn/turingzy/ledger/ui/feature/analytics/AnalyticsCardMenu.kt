package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.component.TopBarIconButton
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cn.turingzy.ledger.model.AnalyticsCard
import cn.turingzy.ledger.model.AnalyticsCardSettings
import kotlin.math.roundToInt

@Composable
internal fun AnalyticsCardMenu(
    settings: AnalyticsCardSettings,
    onEnabledChange: (AnalyticsCard, Boolean) -> Unit,
    onOrderChange: (List<AnalyticsCard>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var draftOrder by remember(settings.order) { mutableStateOf(settings.order) }
    var draggedCard by remember { mutableStateOf<AnalyticsCard?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeight = with(LocalDensity.current) { 48.dp.toPx() }
    val rowStride = with(LocalDensity.current) { 50.dp.toPx() }
    val handleWidth = with(LocalDensity.current) { 40.dp.toPx() }
    val colors = MaterialTheme.colorScheme

    fun move(card: AnalyticsCard, index: Int) {
        draftOrder = AnalyticsCardSettings(order = draftOrder).move(card, index).order
    }

    Box(modifier) {
        TopBarIconButton(
            imageVector = MaterialSymbols.Menu,
            contentDescription = "管理分析卡片",
            onClick = { expanded = !expanded },
            modifier = Modifier.semantics { stateDescription = "已启用${settings.enabledCards.size}张卡片" },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; draggedCard = null; dragOffset = 0f; draftOrder = settings.order },
            shape = RoundedCornerShape(16.dp),
            containerColor = colors.surface,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, colors.outlineVariant),
        ) {
            Column(Modifier.width(216.dp).padding(horizontal = 12.dp)) {
                Column(
                    Modifier.testTag("analyticsCardRows").clip(RoundedCornerShape(12.dp)).pointerInput(settings.order) {
                        detectDragGestures(
                            onDragStart = { position ->
                                if (position.x in 0f..handleWidth && position.y >= 0f && position.y % rowStride < rowHeight) {
                                    draggedCard = draftOrder.getOrNull((position.y / rowStride).toInt())
                                    dragOffset = 0f
                                }
                            },
                            onDrag = { change, delta ->
                                val card = draggedCard ?: return@detectDragGestures
                                change.consume()
                                var index = draftOrder.indexOf(card)
                                var offset = (dragOffset + delta.y).coerceIn(
                                    -index * rowStride,
                                    (draftOrder.lastIndex - index) * rowStride,
                                )
                                while (offset > rowStride / 2 && index < draftOrder.lastIndex) {
                                    move(card, index + 1)
                                    offset -= rowStride
                                    index += 1
                                }
                                while (offset < -rowStride / 2 && index > 0) {
                                    move(card, index - 1)
                                    offset += rowStride
                                    index -= 1
                                }
                                dragOffset = offset
                            },
                            onDragEnd = {
                                if (draggedCard != null && draftOrder != settings.order) onOrderChange(draftOrder)
                                draggedCard = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggedCard = null
                                dragOffset = 0f
                                draftOrder = settings.order
                            },
                        )
                    },
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    draftOrder.forEachIndexed { index, card ->
                        androidx.compose.runtime.key(card) {
                            val enabled = card !in settings.disabled
                            Row(
                                Modifier.testTag("analyticsCard_${card.name}").fillMaxWidth().height(48.dp)
                                    .offset { IntOffset(0, if (draggedCard == card) dragOffset.roundToInt() else 0) }
                                    .zIndex(if (draggedCard == card) 1f else 0f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (draggedCard == card) colors.surfaceVariant.copy(alpha = 0.45f) else Color.Transparent),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier.size(32.dp).semantics {
                                        contentDescription = "拖动${card.title}排序"
                                        customActions = buildList {
                                            if (index > 0) add(CustomAccessibilityAction("上移") { move(card, index - 1); onOrderChange(draftOrder); true })
                                            if (index < draftOrder.lastIndex) add(CustomAccessibilityAction("下移") { move(card, index + 1); onOrderChange(draftOrder); true })
                                        }
                                    },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(MaterialSymbols.DragIndicator, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                }
                                Text(
                                    card.title,
                                    Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (enabled) colors.onSurface else colors.onSurfaceVariant,
                                )
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = { onEnabledChange(card, it) },
                                        modifier = Modifier.scale(0.72f)
                                            .semantics { contentDescription = "启用${card.title}" },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val AnalyticsCard.title: String
    get() = when (this) {
        AnalyticsCard.COMPARISON -> "收支对比"
        AnalyticsCard.TREND -> "收支走势"
        AnalyticsCard.CATEGORIES -> "分类占比"
        AnalyticsCard.AVERAGES -> "日均收支"
    }
