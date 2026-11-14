package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.*
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ConsumptionCalendarScreenTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 14)

    @Test fun selectingDayDisplaysOnlyItsExpensesUsingLedgerRows() {
        val day = today.minusDays(4)
        val category = CategoryWithIcon(Category(1L, "餐饮", CategoryType.EXPENSE, 1L), Icon(1L, "ic_food", "餐饮", "res://"))
        val dashboard = AnalyticsDashboardState(
            days = mapOf(day to AnalyticsDay(12345L, 99900L)), loading = false,
            transactions = listOf(
                AnalyticsTransaction(1L, day, "午餐", "银行卡", 12345L, category = category, time = LocalTime.NOON),
                AnalyticsTransaction(2L, day, "工资", "银行卡", 99900L, TransactionType.INCOME),
            ),
        )
        compose.setContent { LedgerTheme { ConsumptionCalendarContent(dashboard, YearMonth.from(today), {}, today = today) } }
        compose.onNodeWithText("消费日历").assertIsDisplayed()
        compose.onNodeWithText("11月总支出 ¥123.45").assertIsDisplayed()
        compose.onNodeWithText("消费天数").assertDoesNotExist()
        compose.onNodeWithText("单日最高").assertDoesNotExist()
        compose.onNodeWithContentDescription("$day，支出¥123.45").performClick().assertIsSelected()
        compose.onNodeWithText("六").assertIsSelected()
        compose.onNodeWithText("二").assertIsNotSelected()
        compose.onNodeWithText("11月10日 共1笔消费").assertExists()
        compose.onNodeWithContentDescription("${today.plusDays(1)}，支出¥0.00").assertIsNotEnabled()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("银行卡 · 午餐"))
        compose.onNodeWithText("银行卡 · 午餐").assertIsDisplayed()
        compose.onNodeWithText("−¥123.45").assertIsDisplayed()
        compose.onNodeWithText("12:00").assertIsDisplayed()
        compose.onNodeWithText("工资").assertDoesNotExist()
    }

    @Test fun monthPickerAndBackWorkWithoutArrowsOrTodayButton() {
        var back = false
        compose.setContent { LedgerTheme {
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = false), YearMonth.of(2024, 2), { back = true }, today = today)
        } }
        compose.onNodeWithContentDescription("2024-02-29，支出¥0.00").assertExists()
        compose.onNodeWithContentDescription("下个月").assertDoesNotExist()
        compose.onNodeWithContentDescription("上个月").assertDoesNotExist()
        compose.onNodeWithText("今天").assertDoesNotExist()
        compose.onNodeWithText("2024年2月").performClick()
        compose.onNodeWithTag("ledgerPeriodPicker").assertIsDisplayed()
        compose.onNodeWithTag("ledgerPeriod-${LocalDate.of(2024, 3, 1).toEpochDay()}").performClick()
        compose.onNodeWithText("2024年3月").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回分析").performClick()
        compose.runOnIdle { assertTrue(back) }
    }

    @Test fun analyticsTopRightCalendarEntryPassesTheSelectedPeriod() {
        val start = LocalDate.of(2024, 2, 1)
        val state = AnalyticsUiState.Success(AnalyticsTimeScope.MONTH, CategoryType.EXPENSE, "2024年2月",
            0L, 0L, 0L, emptyList(), start)
        var anchor: LocalDate? = null
        compose.setContent { LedgerTheme {
            AnalyticsContent(state, {}, {}, onOpenCalendar = { anchor = it })
        } }
        compose.onNodeWithContentDescription("消费日历热力图").assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(LocalDate.of(2024, 2, 29), anchor) }
    }
}
