package cn.turingzy.ledger.ui.component.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun MonthCalendarDatePickerCard(
    state: CalendarPickerState,
    onDismiss: () -> Unit,
    onConfirm: (startDate: LocalDate, endDate: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(state.dragging, state.edgeDirection) {
        if (!state.dragging || state.edgeDirection == 0) return@LaunchedEffect
        delay(550)
        while (state.dragging && state.edgeDirection != 0) {
            state.turnAtEdge()
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            delay(650)
        }
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.background)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .pointerInput(state) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                    var scale = 1f
                    var switched = false
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } >= 2) {
                            scale *= event.calculateZoom()
                            if (!switched) {
                                val expand = scale > 1.25f
                                val contract = scale < 0.80f
                                if ((expand || contract) && state.zoom(expand = expand)) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    switched = true
                                }
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .padding(horizontal = 10.dp, vertical = 12.dp),
    ) {
        CalendarHeader(state)
        AnimatedContent(
            targetState = state.view,
            transitionSpec = {
                (fadeIn(tween(180)) + scaleIn(tween(220), initialScale = 0.94f)) togetherWith
                    (fadeOut(tween(120)) + scaleOut(tween(180), targetScale = 1.04f))
            },
            label = "calendarView",
        ) { view ->
            CalendarGrid(state, view)
        }
        CalendarFooter(state, onDismiss, onConfirm)
    }
}

@Composable
private fun CalendarHeader(state: CalendarPickerState) {
    Column(
        Modifier.fillMaxWidth().height(44.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.view == CalendarView.DAY) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                CalendarArrow("上一${if (state.view == CalendarView.YEAR) "组年份" else "年"}", false) {
                    state.month = state.month.minusYears(if (state.view == CalendarView.YEAR) 12 else 1)
                }
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable {
                        state.view = if (state.view == CalendarView.MONTH) CalendarView.YEAR else CalendarView.MONTH
                    }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val label = if (state.view == CalendarView.YEAR) {
                        val first = state.month.year - Math.floorMod(state.month.year, 12)
                        "$first – ${first + 11}年"
                    } else "${state.month.year}年"
                    Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                }
                CalendarArrow("下一${if (state.view == CalendarView.YEAR) "组年份" else "年"}", true) {
                    state.month = state.month.plusYears(if (state.view == CalendarView.YEAR) 12 else 1)
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(state: CalendarPickerState, view: CalendarView) {
    // Keep the outgoing animation's cells fixed while the new view is composed.
    val dates = remember(state.month, view) {
        val snapshot = CalendarPickerState(state.month, state.selection)
        snapshot.view = view
        snapshot.gridDates()
    }
    val columns = if (view == CalendarView.DAY) 7 else 4
    val today = remember { LocalDate.now() }
    val colors = MaterialTheme.colorScheme
    val outlineAlpha by animateFloatAsState(if (state.editing) 1f else 0f, tween(160), label = "selectionOutline")
    Column(
        Modifier.fillMaxWidth().height(228.dp)
            .onSizeChanged { state.gridSize = it }
            .semantics {
                contentDescription = when (view) {
                    CalendarView.DAY -> "${state.month.year}年${state.month.monthValue}月，双指展开选择月份"
                    CalendarView.MONTH -> "双指展开选择年份，捏合返回日期"
                    CalendarView.YEAR -> "双指捏合返回月份"
                }
            }
            .pointerInput(state, view) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val initialDate = state.dateAt(down.position)
                    val canDrag = state.beginDrag(initialDate)
                    if (canDrag) down.consume()
                    var moved = false
                    var cancelled = false
                    try {
                        do {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (event.changes.size > 1 || change.isConsumed) {
                                cancelled = true
                                break
                            }
                            if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                            if (moved && canDrag && change.pressed) {
                                state.dragging = true
                                change.consume()
                                state.drag(change.position)
                            }
                        } while (event.changes.any { it.pressed })
                        if (!moved && !cancelled) state.tap(initialDate)
                    } finally {
                        state.dragging = false
                        state.edgeDirection = 0
                    }
                }
            },
    ) {
        dates.chunked(columns).forEach { rowDates ->
            Row(
                Modifier.fillMaxWidth().weight(1f).drawBehind {
                    if (view == CalendarView.YEAR) return@drawBehind
                    val selected = rowDates.indices.filter { index ->
                        val date = rowDates[index]
                        if (view == CalendarView.DAY) date in state.selection else
                            !YearMonth.from(date).isBefore(YearMonth.from(state.selection.start)) &&
                                !YearMonth.from(date).isAfter(YearMonth.from(state.selection.end))
                    }
                    if (selected.isEmpty()) return@drawBehind
                    val cellWidth = size.width / columns
                    val inset = if (view == CalendarView.MONTH) 6.dp.toPx() else 0f
                    val left = selected.first() * cellWidth + inset
                    val selectionHeight = if (view == CalendarView.MONTH) 44.dp.toPx() else 32.dp.toPx()
                    val height = minOf(size.height - 4.dp.toPx(), selectionHeight)
                    val top = (size.height - height) / 2
                    val width = selected.size * cellWidth - inset * 2
                    val endpointWidth = minOf(cellWidth - inset, width)
                    val radius = CornerRadius(minOf(height / 2, 18.dp.toPx()))
                    drawRoundRect(colors.tertiary.copy(alpha = 0.14f), Offset(left, top), Size(width, height), radius)
                    val firstDate = rowDates[selected.first()]
                    val lastDate = rowDates[selected.last()]
                    val starts = if (view == CalendarView.DAY) firstDate == state.selection.start else YearMonth.from(firstDate) == YearMonth.from(state.selection.start)
                    val ends = if (view == CalendarView.DAY) lastDate == state.selection.end else YearMonth.from(lastDate) == YearMonth.from(state.selection.end)
                    if (starts) drawRoundRect(colors.tertiary.copy(alpha = 0.28f), Offset(left, top), Size(endpointWidth, height), radius)
                    if (ends && (!starts || selected.size > 1)) {
                        drawRoundRect(colors.tertiary.copy(alpha = 0.28f), Offset(left + width - endpointWidth, top), Size(endpointWidth, height), radius)
                    }
                    if (outlineAlpha > 0f) {
                        drawRoundRect(
                            colors.tertiary.copy(alpha = outlineAlpha), Offset(left + 1.dp.toPx(), top),
                            Size(width - 2.dp.toPx(), height), radius,
                            style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx()))),
                        )
                        if (starts) drawCircle(colors.tertiary, 3.dp.toPx(), Offset(left + 3.dp.toPx(), size.height / 2))
                        if (ends) drawCircle(colors.tertiary, 3.dp.toPx(), Offset(left + width - 3.dp.toPx(), size.height / 2))
                    }
                },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                rowDates.forEach { date ->
                    val selected = when (view) {
                        CalendarView.DAY -> date in state.selection
                        CalendarView.MONTH -> !YearMonth.from(date).isBefore(YearMonth.from(state.selection.start)) && !YearMonth.from(date).isAfter(YearMonth.from(state.selection.end))
                        CalendarView.YEAR -> date.year == state.month.year
                    }
                    val isCurrentPeriod = when (view) {
                        CalendarView.DAY -> date == today
                        CalendarView.MONTH -> YearMonth.from(date) == YearMonth.from(today)
                        CalendarView.YEAR -> date.year == today.year
                    }
                    Box(
                        Modifier.weight(1f).fillMaxSize().drawBehind {
                            if (isCurrentPeriod) {
                                drawCircle(colors.tertiary, 2.dp.toPx(), Offset(size.width / 2, size.height / 2 + 11.dp.toPx()))
                            }
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = when (view) {
                                CalendarView.DAY -> date.dayOfMonth.toString()
                                CalendarView.MONTH -> "${date.monthValue}月"
                                CalendarView.YEAR -> "${date.year}"
                            },
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (view == CalendarView.DAY && YearMonth.from(date) != state.month && !selected) colors.onSurface.copy(alpha = 0.35f) else colors.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarFooter(
    state: CalendarPickerState,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Close,
                contentDescription = "关闭日期选择器",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.clip(RoundedCornerShape(8.dp))
                .clickable { state.reset(LocalDate.now()) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("重置", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier.clip(RoundedCornerShape(8.dp))
                .clickable { onConfirm(state.selection.start, state.selection.end) }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("确认", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun CalendarArrow(description: String, forward: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}
