package cn.turingzy.ledger.ui.feature.analytics

import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsAxisLabelTest {
    @Test fun subThousandAmountsRetainYuanUnits() {
        assertEquals("¥0", expenseAxisLabel(0L))
        assertEquals("¥500", expenseAxisLabel(50000L))
        assertEquals("¥999.99", expenseAxisLabel(99999L))
    }
    @Test fun thousandsUseCompactKUnitsWithoutTrailingZeros() {
        assertEquals("¥1k", expenseAxisLabel(100000L))
        assertEquals("¥1.25k", expenseAxisLabel(125000L))
        assertEquals("¥10k", expenseAxisLabel(1000000L))
        assertEquals("¥25.5k", expenseAxisLabel(2550000L))
    }
}
