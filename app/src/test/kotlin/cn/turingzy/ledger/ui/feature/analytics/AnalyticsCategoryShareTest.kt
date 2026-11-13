package cn.turingzy.ledger.ui.feature.analytics

import org.junit.Assert.assertEquals
import androidx.compose.ui.graphics.Color
import org.junit.Test

class AnalyticsCategoryShareTest {
    @Test fun percentageUsesTwoDecimalPlacesWithoutTrailingZeros() {
        assertEquals("30.02%", categorySharePercentage(3002L, 10000L))
        assertEquals("40%", categorySharePercentage(120000L, 300000L))
        assertEquals("33.33%", categorySharePercentage(1L, 3L))
    }
    @Test fun emptyAndMaximumAmountsDoNotOverflowOrDivideByZero() {
        assertEquals("0%", categorySharePercentage(0L, 0L))
        assertEquals("100%", categorySharePercentage(Long.MAX_VALUE, Long.MAX_VALUE))
    }
    @Test fun moreThanSixCategoriesGetDistinctColorsInBothThemes() {
        listOf(Color.White, Color.Black).forEach { surface ->
            val colors = categoryShareColors(24, Color.Green, surface)
            assertEquals(24, colors.toSet().size)
        }
    }

}
