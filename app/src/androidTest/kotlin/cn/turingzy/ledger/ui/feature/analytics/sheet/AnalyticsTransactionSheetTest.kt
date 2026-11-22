package cn.turingzy.ledger.ui.feature.analytics.sheet

import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTransaction
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalyticsTransactionSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sharedLedgerRowsShowTimeAndNoteUnderCategoryAndKeepHeaderFixedWhileScrolling() {
        val category = CategoryWithIcon(
            Category(1L, "餐饮", CategoryType.EXPENSE, 1L),
            Icon(1L, "ic_food", "餐饮", "res://"),
        )
        val entries = (1..20).map { index ->
            AnalyticsTransaction(index.toLong(), LocalDate.of(2026, 11, 1), "支出$index", index * 100L,
                category = category, time = LocalTime.of(8, 30).plusMinutes(index - 1L))
        }
        var dismissed = false
        compose.setContent {
            LedgerTheme {
                AnalyticsTransactionSheet("11月支出明细", "2026-11-01 至 2026-11-30", entries, { dismissed = true })
            }
        }
        val categoryBounds = compose.onAllNodesWithText("餐饮")[0].getUnclippedBoundsInRoot()
        val timeBounds = compose.onNodeWithText("08:30 · 支出1").assertIsDisplayed().getUnclippedBoundsInRoot()
        val amountBounds = compose.onNodeWithText("−¥1.00").assertIsDisplayed().getUnclippedBoundsInRoot()
        assertTrue(timeBounds.left == categoryBounds.left && timeBounds.top >= categoryBounds.bottom)
        assertTrue(timeBounds.left < amountBounds.left && timeBounds.top >= amountBounds.bottom)
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("08:49 · 支出20"))
        compose.onNodeWithText("08:49 · 支出20").assertIsDisplayed()
        val title = compose.onNodeWithText("11月支出明细").assertIsDisplayed()
        val close = compose.onNodeWithContentDescription("关闭交易明细").assertIsDisplayed()
        assertTrue(close.getUnclippedBoundsInRoot().left < title.getUnclippedBoundsInRoot().left)
        close.performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }
    @Test fun amountOrderKeepsLargestFirstAcrossDatesWithoutDateGroups() {
        val date = LocalDate.of(2026, 11, 1)
        val entries = listOf(
            AnalyticsTransaction(1L, date.plusDays(2), "小额", 100L),
            AnalyticsTransaction(2L, date, "大额", 300L),
            AnalyticsTransaction(3L, date.plusDays(1), "中额", 200L),
        )
        compose.setContent { LedgerTheme {
            AnalyticsTransactionSheet("餐饮", "2026-11-01 至 2026-11-30", entries, {}, groupByDate = false)
        } }
        val largest = compose.onNodeWithText("−¥3.00").getUnclippedBoundsInRoot()
        val middle = compose.onNodeWithText("−¥2.00").getUnclippedBoundsInRoot()
        val smallest = compose.onNodeWithText("−¥1.00").getUnclippedBoundsInRoot()
        assertTrue(largest.top < middle.top && middle.top < smallest.top)
        compose.onNodeWithText("11/01", substring = true).assertDoesNotExist()
        compose.onNodeWithText("11.01").assertIsDisplayed()
        compose.onNodeWithText("总支出 ¥6.00").assertIsDisplayed()
    }

    @Test fun mixedMonthlyTransactionsShowBothTotalsOnTheSameLine() {
        val date = LocalDate.of(2026, 11, 1)
        val entries = listOf(
            AnalyticsTransaction(1L, date, "餐饮", 12345L),
            AnalyticsTransaction(2L, date.plusDays(1), "工资", 67890L,
                type = cn.turingzy.ledger.model.transaction.TransactionType.INCOME),
        )
        compose.setContent { LedgerTheme {
            AnalyticsTransactionSheet("11月账单", "2026-11-01 至 2026-11-30", entries, {}, groupByDate = false)
        } }
        compose.onNodeWithText("总支出 ¥123.45  总收入 ¥678.90").assertIsDisplayed()
    }

}
