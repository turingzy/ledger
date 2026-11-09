package cn.turingzy.ledger.ui.component.calendar

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.floor

internal enum class CalendarView { DAY, MONTH, YEAR }

@Stable
internal class CalendarPickerState(initialMonth: YearMonth, initialSelection: CalendarSelection) {
    var month by mutableStateOf(initialMonth)
    var selection by mutableStateOf(initialSelection)
    var view by mutableStateOf(CalendarView.DAY)
    var editing by mutableStateOf(false)
    var dragging by mutableStateOf(false)
    var startHandle by mutableStateOf(false)
    var edgeDirection by mutableIntStateOf(0)
    var gridSize by mutableStateOf(IntSize.Zero)
    var pointer by mutableStateOf(Offset.Zero)
    private var sharedHandle = false

    fun zoom(expand: Boolean): Boolean {
        val target = when {
            expand && view == CalendarView.DAY -> CalendarView.MONTH
            expand && view == CalendarView.MONTH -> CalendarView.YEAR
            !expand && view == CalendarView.YEAR -> CalendarView.MONTH
            !expand && view == CalendarView.MONTH -> CalendarView.DAY
            else -> return false
        }
        dragging = false
        edgeDirection = 0
        view = target
        if (target == CalendarView.DAY && selection.start.year == month.year) {
            month = YearMonth.from(selection.start)
        }
        return true
    }

    fun reset(today: LocalDate) {
        selection = when (view) {
            CalendarView.DAY -> CalendarSelection.week(today)
            CalendarView.MONTH -> CalendarSelection.month(YearMonth.from(today))
            CalendarView.YEAR -> CalendarSelection.year(today.year)
        }
        month = YearMonth.from(today)
        editing = false
        dragging = false
        edgeDirection = 0
        sharedHandle = false
    }

    fun gridDates(): List<LocalDate> = when (view) {
        CalendarView.DAY -> {
            val first = month.atDay(1)
            val start = first.minusDays(first.dayOfWeek.value - 1L)
            List(42) { start.plusDays(it.toLong()) }
        }
        CalendarView.MONTH -> List(12) { YearMonth.of(month.year, it + 1).atDay(1) }
        CalendarView.YEAR -> {
            val firstYear = month.year - Math.floorMod(month.year, 12)
            List(12) { LocalDate.of(firstYear + it, 1, 1) }
        }
    }

    fun dateAt(position: Offset): LocalDate {
        val columns = if (view == CalendarView.DAY) 7 else 4
        val rows = if (view == CalendarView.DAY) 6 else 3
        val column = floor(position.x / (gridSize.width.toFloat() / columns)).toInt().coerceIn(0, columns - 1)
        val row = floor(position.y / (gridSize.height.toFloat() / rows)).toInt().coerceIn(0, rows - 1)
        val index = row * columns + column
        return when (view) {
            CalendarView.DAY -> month.atDay(1).let { it.minusDays(it.dayOfWeek.value - 1L).plusDays(index.toLong()) }
            CalendarView.MONTH -> YearMonth.of(month.year, index + 1).atDay(1)
            CalendarView.YEAR -> LocalDate.of(month.year - Math.floorMod(month.year, 12) + index, 1, 1)
        }
    }

    fun isSelected(date: LocalDate): Boolean = when (view) {
        CalendarView.DAY -> date in selection
        CalendarView.MONTH -> !YearMonth.from(date).isBefore(YearMonth.from(selection.start)) &&
            !YearMonth.from(date).isAfter(YearMonth.from(selection.end))
        CalendarView.YEAR -> date.year == month.year
    }

    fun tap(date: LocalDate) {
        if (view == CalendarView.YEAR) {
            month = month.withYear(date.year)
            view = CalendarView.MONTH
            return
        }
        if (isSelected(date)) {
            editing = !editing
        } else {
            selection = if (view == CalendarView.DAY) CalendarSelection.week(date) else CalendarSelection.month(YearMonth.from(date))
            editing = false
        }
    }

    fun beginDrag(date: LocalDate): Boolean {
        if (!editing || view == CalendarView.YEAR) return false
        val isStart = if (view == CalendarView.MONTH) YearMonth.from(date) == YearMonth.from(selection.start) else date == selection.start
        val isEnd = if (view == CalendarView.MONTH) YearMonth.from(date) == YearMonth.from(selection.end) else date == selection.end
        if (!isStart && !isEnd) return false
        sharedHandle = isStart && isEnd
        startHandle = isStart && !isEnd
        return true
    }

    fun drag(position: Offset) {
        pointer = position
        val date = dateAt(position)
        val sameBoundary = if (view == CalendarView.MONTH) YearMonth.from(date) == YearMonth.from(selection.start) else date == selection.start
        if (sharedHandle && !sameBoundary) {
            startHandle = date.isBefore(selection.start)
            sharedHandle = false
        }
        val edge = gridSize.height / (if (view == CalendarView.DAY) 6f else 3f) * 0.22f
        edgeDirection = when {
            position.y < edge || position.x < 0 -> -1
            position.y > gridSize.height - edge || position.x > gridSize.width -> 1
            else -> 0
        }
        selection = selection.resize(date, startHandle, view == CalendarView.MONTH)
    }

    fun turnAtEdge() {
        month = if (view == CalendarView.DAY) month.plusMonths(edgeDirection.toLong()) else month.plusYears(edgeDirection.toLong())
        val date = dateAt(pointer)
        selection = selection.resize(date, startHandle, view == CalendarView.MONTH)
    }
}

