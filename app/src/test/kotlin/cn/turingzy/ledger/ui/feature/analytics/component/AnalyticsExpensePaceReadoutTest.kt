package cn.turingzy.ledger.ui.feature.analytics.component

import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTimeScope
import cn.turingzy.ledger.ui.feature.analytics.calculation.expensePaceChangeLabel
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsExpensePaceReadoutTest {
    @Test fun readoutUsesTheGranularityOfTheSelectedPeriod() {
        val date = LocalDate.of(2024, 11, 1)
        assertEquals("11月", expensePaceReadoutDate(AnalyticsTimeScope.YEAR, date))
        assertEquals("2024", expensePaceReadoutDate(AnalyticsTimeScope.ALL, date))
        assertEquals("11.1", expensePaceReadoutDate(AnalyticsTimeScope.MONTH, date))
    }

    @Test fun comparesCumulativeExpensesRatherThanDailyAverages() {
        assertEquals("增长 68%", expensePaceChangeLabel(210000L, 125000L))
        assertEquals("下降 50%", expensePaceChangeLabel(500L, 1000L))
        assertEquals("持平", expensePaceChangeLabel(100L, 100L))
    }

    @Test fun zeroBaselineHasNoInventedGrowthRate() {
        assertEquals("暂无环比", expensePaceChangeLabel(100L, 0L))
        assertEquals("持平", expensePaceChangeLabel(0L, 0L))
        assertEquals("下降 100%", expensePaceChangeLabel(0L, 100L))
    }

    @Test fun tinyChangesAndMaximumAmountsRetainPrecision() {
        assertEquals("增长 <0.1%", expensePaceChangeLabel(100001L, 100000L))
        assertEquals("下降 <0.1%", expensePaceChangeLabel(100000L, 100001L))
        assertEquals("增长 <0.1%", expensePaceChangeLabel(Long.MAX_VALUE, Long.MAX_VALUE - 1))
    }
}
