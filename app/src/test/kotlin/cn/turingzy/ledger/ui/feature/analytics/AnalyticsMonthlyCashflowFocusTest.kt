package cn.turingzy.ledger.ui.feature.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnalyticsMonthlyCashflowFocusTest {
    @Test fun scrollingToEitherBoundarySelectsTheFirstOrLastMonth() {
        assertEquals(0, monthlyCashflowFocusIndex(0, 600, 600f, 100f, 12))
        assertEquals(11, monthlyCashflowFocusIndex(600, 600, 600f, 100f, 12))
    }

    @Test fun intermediateOffsetsContinuouslySelectEveryMonthIncludingThoseNearTheEdges() {
        assertEquals(0, monthlyCashflowFocusIndex(1, 600, 600f, 100f, 12))
        assertEquals(6, monthlyCashflowFocusIndex(300, 600, 600f, 100f, 12))
        assertEquals(11, monthlyCashflowFocusIndex(599, 600, 600f, 100f, 12))
        listOf(7, 12, 24).forEach { count ->
            val maximumOffset = (count - 6) * 100
            val selected = (0..maximumOffset).map { offset ->
                monthlyCashflowFocusIndex(offset, maximumOffset, 600f, 100f, count)
            }
            assertEquals((0 until count).toSet(), selected.toSet())
            assertEquals(selected, selected.sortedBy { it })
        }
    }

    @Test fun nonScrollingAndUnmeasuredChartsRemainSafe() {
        assertEquals(3, monthlyCashflowFocusIndex(0, 0, 600f, 100f, 6))
        assertNull(monthlyCashflowFocusIndex(0, 600, 0f, 0f, 12))
        assertNull(monthlyCashflowFocusIndex(0, 600, 600f, 100f, 0))
    }
}
