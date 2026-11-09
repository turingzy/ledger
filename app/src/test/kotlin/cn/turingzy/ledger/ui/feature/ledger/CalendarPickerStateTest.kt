package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarPickerStateTest {
    private val initial = CalendarSelection.week(LocalDate.of(2026, 11, 9))
    private fun state() = CalendarPickerState(YearMonth.of(2026, 11), initial).apply {
        gridSize = IntSize(700, 600)
    }

    @Test
    fun tapSelectsWholeWeekThenEnablesEditing() {
        val state = state()
        state.tap(LocalDate.of(2026, 11, 25))
        assertEquals(CalendarSelection.week(LocalDate.of(2026, 11, 25)), state.selection)
        assertFalse(state.editing)
        assertFalse(state.beginDrag(state.selection.start))
        state.tap(state.selection.start)
        assertTrue(state.editing)
        assertTrue(state.beginDrag(state.selection.start))
        assertFalse(state.beginDrag(state.selection.start.plusDays(2)))
    }

    @Test
    fun pinchOnlyChangesViewAndPreservesExactDates() {
        val state = state()
        state.zoom(expand = true)
        assertEquals(CalendarView.MONTH, state.view)
        assertEquals(initial, state.selection)
        state.zoom(expand = false)
        assertEquals(CalendarView.DAY, state.view)
        assertEquals(initial, state.selection)
    }

    @Test
    fun choosingYearOnlyNavigatesAndDoesNotChangeSelection() {
        val state = state()
        state.view = CalendarView.YEAR
        state.tap(LocalDate.of(2028, 1, 1))
        assertEquals(YearMonth.of(2028, 11), state.month)
        assertEquals(CalendarView.MONTH, state.view)
        assertEquals(initial, state.selection)
    }

    @Test
    fun dragContinuesAcrossMonthWithoutRestartingHandle() {
        val state = state()
        state.editing = true
        assertTrue(state.beginDrag(initial.end))
        state.drag(Offset(650f, 610f))
        assertEquals(1, state.edgeDirection)
        state.turnAtEdge()
        assertEquals(YearMonth.of(2026, 12), state.month)
        assertEquals(LocalDate.of(2027, 1, 10), state.selection.end)
        state.drag(Offset(350f, 250f))
        assertEquals(LocalDate.of(2026, 12, 17), state.selection.end)
        assertEquals(initial.start, state.selection.start)
    }

    @Test
    fun singleMonthCanExpandUsingEitherBoundary() {
        val state = state()
        state.zoom(expand = true)
        state.tap(LocalDate.of(2026, 6, 1))
        state.editing = true
        assertTrue(state.beginDrag(LocalDate.of(2026, 6, 1)))
        state.drag(Offset(250f, 250f))
        state.drag(Offset(50f, 250f))
        assertEquals(CalendarSelection(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 6, 30)), state.selection)
    }

    @Test
    fun expandingAndContractingTraversesOneLevelWithoutChangingRange() {
        val state = state()
        assertFalse(state.zoom(expand = false))
        assertTrue(state.zoom(expand = true))
        assertEquals(CalendarView.MONTH, state.view)
        assertTrue(state.zoom(expand = true))
        assertEquals(CalendarView.YEAR, state.view)
        assertFalse(state.zoom(expand = true))
        assertTrue(state.zoom(expand = false))
        assertEquals(CalendarView.MONTH, state.view)
        assertTrue(state.zoom(expand = false))
        assertEquals(CalendarView.DAY, state.view)
        assertEquals(initial, state.selection)
    }

    @Test
    fun resetUsesTheCurrentViewAndReturnsToToday() {
        val today = LocalDate.of(2028, 2, 29)
        val expected = mapOf(
            CalendarView.DAY to CalendarSelection(LocalDate.of(2028, 2, 28), LocalDate.of(2028, 3, 5)),
            CalendarView.MONTH to CalendarSelection(LocalDate.of(2028, 2, 1), today),
            CalendarView.YEAR to CalendarSelection(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31)),
        )
        expected.forEach { (view, range) ->
            val state = state()
            state.view = view
            state.editing = true
            state.dragging = true
            state.edgeDirection = 1
            state.reset(today)
            assertEquals(range, state.selection)
            assertEquals(YearMonth.of(2028, 2), state.month)
            assertEquals(view, state.view)
            assertFalse(state.editing)
            assertFalse(state.dragging)
            assertEquals(0, state.edgeDirection)
        }
    }
}
