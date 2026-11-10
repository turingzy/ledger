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
internal class CalendarPickerState(
    initialMonth: YearMonth,
    initialSelection: CalendarSelection,
    private val resetSelection: ((LocalDate) -> CalendarSelection)? = null,
) {
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
        selection = resetSelection?.invoke(today) ?: when (view) {
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
        CalendarView.YEAR -> date.year in selection.start.year..selection.end.year
    }

    fun tap(date: LocalDate) {
        if (isSelected(date)) {
            editing = !editing
        } else {
            selection = when (view) {
                CalendarView.DAY -> CalendarSelection.week(date)
                CalendarView.MONTH -> CalendarSelection.month(YearMonth.from(date))
                CalendarView.YEAR -> {
                    month = month.withYear(date.year)
                    CalendarSelection.year(date.year)
                }
            }
            editing = false
        }
    }

    fun beginDrag(date: LocalDate): Boolean {
        if (!editing) return false
        val isStart = samePeriod(date, selection.start)
        val isEnd = samePeriod(date, selection.end)
        if (!isStart && !isEnd) return false
        sharedHandle = isStart && isEnd
        startHandle = isStart && !isEnd
        return true
    }

    fun drag(position: Offset) {
        pointer = position
        val date = dateAt(position)
        val sameBoundary = samePeriod(date, selection.start)
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
        resizeSelection(date)
    }

    fun turnAtEdge() {
        month = when (view) {
            CalendarView.DAY -> month.plusMonths(edgeDirection.toLong())
            CalendarView.MONTH -> month.plusYears(edgeDirection.toLong())
            CalendarView.YEAR -> month.plusYears(edgeDirection.toLong() * 12)
        }
        val date = dateAt(pointer)
        resizeSelection(date)
    }

    private fun samePeriod(date: LocalDate, boundary: LocalDate): Boolean = when (view) {
        CalendarView.DAY -> date == boundary
        CalendarView.MONTH -> YearMonth.from(date) == YearMonth.from(boundary)
        CalendarView.YEAR -> date.year == boundary.year
    }

    private fun resizeSelection(date: LocalDate) {
        selection = if (view == CalendarView.YEAR) {
            selection.resizeYears(date, startHandle)
        } else {
            selection.resize(date, startHandle, view == CalendarView.MONTH)
        }
    }
}

