package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalyticsMonthlyCashflowCardTest {
    @get:Rule val compose = createComposeRule()

    @Test fun monthSelectionShowsSummaryAndOpensMonthlyTransactions() {
        compose.setContent {
            LedgerTheme {
                MonthlyCashflowCard(mapOf(
                    LocalDate.of(2026, 1, 10) to AnalyticsDay(expense = 1500L, income = 1000L),
                    LocalDate.of(2026, 2, 10) to AnalyticsDay(expense = 1000L, income = 3000L),
                ), YearMonth.of(2026, 2))
            }
        }
        val title = compose.onNodeWithText("收支与结余").getUnclippedBoundsInRoot()
        val expense = compose.onNodeWithText("支出").getUnclippedBoundsInRoot()
        val income = compose.onNodeWithText("收入").getUnclippedBoundsInRoot()
        val balance = compose.onNodeWithText("结余").getUnclippedBoundsInRoot()
        assertTrue(title.right < expense.left)
        assertTrue(expense.right < income.left)
        assertTrue(income.right < balance.left)
        compose.onNodeWithContentDescription("收入支出双柱与结余折线组合图").assertHasNoClickAction()
            .performTouchInput { click(center) }
        compose.onNodeWithContentDescription("2026年2月").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithText("净结余").assertIsDisplayed()
        compose.onNodeWithText("储蓄率").assertDoesNotExist()
        val chart = compose.onNodeWithContentDescription("收入支出双柱与结余折线组合图").getUnclippedBoundsInRoot()
        val summary = compose.onNodeWithContentDescription("2026年2月收支汇总").getUnclippedBoundsInRoot()
        assertTrue(summary.top >= chart.top && summary.bottom <= chart.bottom)
        compose.onNodeWithContentDescription("2026年2月收支汇总").performClick()
        compose.onNodeWithText("2026年2月账单").assertIsDisplayed()
        compose.onNodeWithText("暂无交易记录").assertIsDisplayed()
        compose.onNodeWithContentDescription("关闭交易明细").performClick()
    }
    @Test fun summaryTogglesAndAutomaticallyDisappears() {
        compose.setContent {
            LedgerTheme { MonthlyCashflowCard(emptyMap(), YearMonth.of(2026, 2)) }
        }
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("2026年2月").performClick()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithContentDescription("2026年2月收支汇总").assertIsDisplayed()
        compose.onNodeWithContentDescription("2026年2月").performClick()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithContentDescription("2026年2月收支汇总").assertDoesNotExist()
        compose.onNodeWithContentDescription("2026年2月").performClick()
        compose.mainClock.advanceTimeBy(200)
        compose.onNodeWithContentDescription("2026年2月收支汇总").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(2800)
        compose.onNodeWithContentDescription("2026年2月收支汇总").assertIsDisplayed()
        compose.mainClock.advanceTimeBy(2500)
        compose.onNodeWithContentDescription("2026年2月收支汇总").assertDoesNotExist()
    }

}
