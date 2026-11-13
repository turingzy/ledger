package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalyticsStatisticsCardTest {
    @get:Rule val compose = createComposeRule()
    private val start = LocalDate.of(2026, 11, 1)
    private val state = AnalyticsUiState.Success(
        AnalyticsTimeScope.MONTH, CategoryType.EXPENSE, "", 600000L, 950000L, 350000L,
        emptyList(), start, start.plusDays(29),
        comparison = AnalyticsComparison(start.minusMonths(1), start.minusDays(1), 310000L, 0L),
    )
    @Test fun twoRowsShowTotalsAverageAndTrendingUp() {
        compose.setContent { LedgerTheme { AnalyticsStatisticsCard(state, emptyMap()) } }
        compose.onNodeWithText("收支统计").assertIsDisplayed()
        listOf("¥6000.00", "¥9500.00", "¥3500.00", "¥200.00", "日均支出", "100%").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithContentDescription("环比增长").assertIsDisplayed()
        compose.onNodeWithText("环比增长").assertDoesNotExist()
        val expense = compose.onNodeWithText("支出").getUnclippedBoundsInRoot()
        val income = compose.onNodeWithText("收入").getUnclippedBoundsInRoot()
        val balance = compose.onNodeWithText("结余").getUnclippedBoundsInRoot()
        val average = compose.onNodeWithText("日均支出").getUnclippedBoundsInRoot()
        assertEquals(expense.top, income.top)
        assertEquals(balance.top, average.top)
        assertTrue(expense.right < income.left)
        assertTrue(expense.bottom < balance.top)
    }
    @Test fun decreasingExpenseUsesTrendingDownAndAnnualLabel() {
        compose.setContent {
            LedgerTheme {
                AnalyticsStatisticsCard(state.copy(timeScope = AnalyticsTimeScope.YEAR,
                    periodStart = LocalDate.of(2026, 1, 1), periodEnd = LocalDate.of(2026, 12, 31),
                    comparison = AnalyticsComparison(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31), 1200000L, 0L)), emptyMap())
            }
        }
        compose.onNodeWithText("月均支出").assertIsDisplayed()
        compose.onNodeWithText("50%").assertIsDisplayed()
        compose.onNodeWithContentDescription("环比下降").assertIsDisplayed()
        compose.onNodeWithText("环比下降").assertDoesNotExist()
        val label = compose.onNodeWithText("¥500.00").getUnclippedBoundsInRoot()
        val trend = compose.onNodeWithText("50%").getUnclippedBoundsInRoot()
        assertTrue(trend.left > label.right)
        assertTrue(trend.top < label.bottom && trend.bottom > label.top)
    }
    @Test fun equalAndUnavailableComparisonsShowNoTrendOrPlaceholder() {
        val value = mutableStateOf(state.copy(comparison = state.comparison?.copy(totalExpense = 620000L)))
        compose.setContent { LedgerTheme { AnalyticsStatisticsCard(value.value, emptyMap()) } }
        fun assertNoTrend() {
            compose.onNodeWithText("环比持平").assertDoesNotExist()
            compose.onNodeWithText("暂无环比").assertDoesNotExist()
            compose.onNodeWithContentDescription("环比增长").assertDoesNotExist()
            compose.onNodeWithContentDescription("环比下降").assertDoesNotExist()
            compose.onNodeWithText("¥200.00").assertIsDisplayed()
        }
        assertNoTrend()
        compose.runOnIdle { value.value = state.copy(comparison = state.comparison?.copy(totalExpense = 0L)) }
        assertNoTrend()
    }

}
