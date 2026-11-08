package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.ui.theme.IosBg
import cn.turingzy.ledger.ui.theme.IosLabel
import cn.turingzy.ledger.ui.theme.IosSecondaryLabel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private const val PAGER_BASE_PAGE = 1200
private const val PAGER_TOTAL_PAGES = 2400

private enum class CalendarDragHandle {
    NONE, START, END, BOTH
}

@Composable
fun MonthCalendarDatePickerCard(
    currentYear: Int,
    currentMonth: Int,
    filterStartDate: LocalDate?,
    filterEndDate: LocalDate?,
    onMonthChanged: (year: Int, month: Int) -> Unit,
    onConfirm: (startDate: LocalDate, endDate: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialYearMonth = remember { YearMonth.of(currentYear, currentMonth) }
    val pagerState = rememberPagerState(
        initialPage = PAGER_BASE_PAGE,
    ) { PAGER_TOTAL_PAGES }

    // 默认框选当前一周 (周一至周日)
    var stagedStartDate by remember(filterStartDate) {
        mutableStateOf(
            filterStartDate ?: LocalDate.now().with(DayOfWeek.MONDAY),
        )
    }
    var stagedEndDate by remember(filterEndDate) {
        mutableStateOf(
            filterEndDate ?: LocalDate.now().with(DayOfWeek.SUNDAY),
        )
    }

    // 计算当前 Pager 所在的年月
    val currentPagerYearMonth = remember(pagerState.currentPage) {
        val monthOffset = (pagerState.currentPage - PAGER_BASE_PAGE).toLong()
        initialYearMonth.plusMonths(monthOffset)
    }

    // 翻页时顶栏月份同步变化
    LaunchedEffect(currentPagerYearMonth) {
        if ((currentPagerYearMonth.year != currentYear) || (currentPagerYearMonth.monthValue != currentMonth)) {
            onMonthChanged(currentPagerYearMonth.year, currentPagerYearMonth.monthValue)
        }
    }

    // 外部年份月份变动时同步更新 Pager 位置
    LaunchedEffect(currentYear, currentMonth) {
        val targetOffset = ((currentYear - initialYearMonth.year) * 12) + (currentMonth - initialYearMonth.monthValue)
        val targetPage = PAGER_BASE_PAGE + targetOffset
        if ((pagerState.currentPage != targetPage) && (targetPage in (0 until PAGER_TOTAL_PAGES))) {
            pagerState.scrollToPage(targetPage)
        }
    }

    // 卡片背景与主界面一致 (IosBg)，线条加黑 (Color(0x4D000000))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(IosBg)
            .border(
                width = 1.dp,
                color = Color(0x4D000000),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 星期标题栏：一  二  三  四  五  六  日
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val weekDayHeaders = listOf("一", "二", "三", "四", "五", "六", "日")
                weekDayHeaders.forEach { header ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = header,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = IosSecondaryLabel,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 水平翻转月视图 (左右滑动翻页)
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(228.dp),
            ) { page ->
                val pageYearMonth = remember(page) {
                    val monthOffset = (page - PAGER_BASE_PAGE).toLong()
                    initialYearMonth.plusMonths(monthOffset)
                }

                MonthCalendarGrid(
                    yearMonth = pageYearMonth,
                    selectedStartDate = stagedStartDate,
                    selectedEndDate = stagedEndDate,
                    onSetStartDate = { stagedStartDate = it },
                    onSetEndDate = { stagedEndDate = it },
                )
            }

            // 底部操作区：去除具体天数提示，采用同顶栏日期风格的2号字，点击确认后生效
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val rangeText = "${stagedStartDate.monthValue}.${stagedStartDate.dayOfMonth.toString().padStart(2, '0')} - ${stagedEndDate.monthValue}.${stagedEndDate.dayOfMonth.toString().padStart(2, '0')}"

                Text(
                    text = rangeText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosSecondaryLabel,
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF34C759))
                        .clickable {
                            onConfirm(stagedStartDate, stagedEndDate)
                        }
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "确认",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthCalendarGrid(
    yearMonth: YearMonth,
    selectedStartDate: LocalDate,
    selectedEndDate: LocalDate,
    onSetStartDate: (LocalDate) -> Unit,
    onSetEndDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstOfMonth = remember(yearMonth) { yearMonth.atDay(1) }
    val daysBefore = remember(firstOfMonth) { firstOfMonth.dayOfWeek.value - 1 }
    val gridStartDate = remember(firstOfMonth, daysBefore) {
        firstOfMonth.minusDays(daysBefore.toLong())
    }

    // 6 行 7 列 = 42 个日期
    val weeks = remember(gridStartDate) {
        (0 until 6).map { weekIndex ->
            (0 until 7).map { dayIndex ->
                gridStartDate.plusDays(((weekIndex * 7) + dayIndex).toLong())
            }
        }
    }

    var gridWidth by remember { mutableFloatStateOf(0f) }
    var gridHeight by remember { mutableFloatStateOf(0f) }
    var activeHandle by remember { mutableStateOf(CalendarDragHandle.NONE) }
    var dragStartOffset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                gridWidth = coordinates.size.width.toFloat()
                gridHeight = coordinates.size.height.toFloat()
            }
            .pointerInput(selectedStartDate, selectedEndDate, weeks) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { startOffset ->
                        if (gridWidth > 0f && gridHeight > 0f) {
                            dragStartOffset = startOffset
                            val colWidth = gridWidth / 7f
                            val rowHeight = gridHeight / 6f
                            val col = (startOffset.x / colWidth).toInt().coerceIn(0, 6)
                            val row = (startOffset.y / rowHeight).toInt().coerceIn(0, 5)
                            val touchedDate = weeks[row][col]

                            val isStart = touchedDate == selectedStartDate
                            val isEnd = touchedDate == selectedEndDate

                            activeHandle = when {
                                isStart && isEnd -> CalendarDragHandle.BOTH
                                isStart -> CalendarDragHandle.START
                                isEnd -> CalendarDragHandle.END
                                else -> CalendarDragHandle.NONE
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        if (gridWidth > 0f && gridHeight > 0f && activeHandle != CalendarDragHandle.NONE) {
                            val colWidth = gridWidth / 7f
                            val rowHeight = gridHeight / 6f
                            val col = (change.position.x / colWidth).toInt().coerceIn(0, 6)
                            val row = (change.position.y / rowHeight).toInt().coerceIn(0, 5)
                            val hoveredDate = weeks[row][col]

                            when (activeHandle) {
                                CalendarDragHandle.BOTH -> {
                                    if (change.position.x < dragStartOffset.x) {
                                        activeHandle = CalendarDragHandle.START
                                        if (!hoveredDate.isAfter(selectedEndDate)) {
                                            onSetStartDate(hoveredDate)
                                        }
                                    } else if (change.position.x > dragStartOffset.x) {
                                        activeHandle = CalendarDragHandle.END
                                        if (!hoveredDate.isBefore(selectedStartDate)) {
                                            onSetEndDate(hoveredDate)
                                        }
                                    }
                                }
                                CalendarDragHandle.START -> {
                                    // 随手指位置移动实时改变开始日期 (向左向右自由伸缩，不超过结束日期)
                                    if (!hoveredDate.isAfter(selectedEndDate)) {
                                        onSetStartDate(hoveredDate)
                                    }
                                }
                                CalendarDragHandle.END -> {
                                    // 随手指位置移动实时改变结束日期 (向右向左自由伸缩，不小于开始日期)
                                    if (!hoveredDate.isBefore(selectedStartDate)) {
                                        onSetEndDate(hoveredDate)
                                    }
                                }
                                CalendarDragHandle.NONE -> {}
                            }
                        }
                    },
                    onDragEnd = {
                        activeHandle = CalendarDragHandle.NONE
                    },
                    onDragCancel = {
                        activeHandle = CalendarDragHandle.NONE
                    },
                )
            },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            weeks.forEach { weekDates ->
                val weekStart = weekDates.first()
                val weekEnd = weekDates.last()

                // 判断此周行是否与选中的起止日期区间相交
                val hasOverlap = !weekEnd.isBefore(selectedStartDate) && !weekStart.isAfter(selectedEndDate)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // 1. 伸缩框 (胶囊状浅薄荷绿，左右端深色全覆盖无缝隙，已移除外侧箭头)
                    if (hasOverlap) {
                        val startCol = if (selectedStartDate.isBefore(weekStart)) 0 else java.time.temporal.ChronoUnit.DAYS.between(weekStart, selectedStartDate).toInt().coerceIn(0, 6)
                        val endCol = if (selectedEndDate.isAfter(weekEnd)) 6 else java.time.temporal.ChronoUnit.DAYS.between(weekStart, selectedEndDate).toInt().coerceIn(0, 6)
                        val colSpan = ((endCol - startCol) + 1).coerceAtLeast(1)

                        val isStartInThisRow = !selectedStartDate.isBefore(weekStart) && !selectedStartDate.isAfter(weekEnd)
                        val isEndInThisRow = !selectedEndDate.isBefore(weekStart) && !selectedEndDate.isAfter(weekEnd)

                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (startCol > 0) {
                                Spacer(modifier = Modifier.weight(startCol.toFloat()))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(colSpan.toFloat())
                                    .height(34.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                // 浅薄荷绿整体胶囊底衬
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(percent = 50))
                                        .background(Color(0xFFCEF0DA)),
                                )

                                // 起始与结束日期深色全覆盖 (无圆周留缝，完整贴合左右胶囊圆弧端)
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (isStartInThisRow) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .clip(
                                                    if (colSpan == 1) {
                                                        RoundedCornerShape(percent = 50)
                                                    } else {
                                                        RoundedCornerShape(
                                                            topStartPercent = 50,
                                                            bottomStartPercent = 50,
                                                            topEndPercent = 0,
                                                            bottomEndPercent = 0,
                                                        )
                                                    },
                                                )
                                                .background(Color(0xFFA1E3B8)),
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }

                                    if (colSpan > 2) {
                                        Spacer(modifier = Modifier.weight((colSpan - 2).toFloat()))
                                    }

                                    if (colSpan > 1) {
                                        if (isEndInThisRow) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(
                                                        RoundedCornerShape(
                                                            topStartPercent = 0,
                                                            bottomStartPercent = 0,
                                                            topEndPercent = 50,
                                                            bottomEndPercent = 50,
                                                        ),
                                                    )
                                                    .background(Color(0xFFA1E3B8)),
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }

                            if (endCol < 6) {
                                Spacer(modifier = Modifier.weight((6 - endCol).toFloat()))
                            }
                        }
                    }

                    // 2. 日期数字层 (7列平铺，仅展示日期，点击不触发伸缩框，完全通过长按拖拽随手改变)
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val today = remember { LocalDate.now() }

                        weekDates.forEach { date ->
                            val isCurrentMonth = date.monthValue == yearMonth.monthValue
                            val isToday = date == today
                            val isSelected = !date.isBefore(selectedStartDate) && !date.isAfter(selectedEndDate)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                                        color = when {
                                            !isCurrentMonth -> Color(0x38000000)
                                            isSelected -> IosLabel
                                            isToday -> Color(0xFF34C759)
                                            else -> IosLabel
                                        },
                                        textAlign = TextAlign.Center,
                                    )

                                    if (isToday && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 1.dp)
                                                .size(3.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF34C759)),
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
}

@Preview(showBackground = true)
@Composable
fun MonthCalendarDatePickerCardPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(IosBg)
            .padding(16.dp),
    ) {
        MonthCalendarDatePickerCard(
            currentYear = 2026,
            currentMonth = 11,
            filterStartDate = LocalDate.of(2026, 11, 2),
            filterEndDate = LocalDate.of(2026, 11, 8),
            onMonthChanged = { _, _ -> },
            onConfirm = { _, _ -> },
        )
    }
}
