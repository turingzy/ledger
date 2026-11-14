package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalyticsTransactionSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sharedLedgerRowsRetainCategoryNoteTimeAndFixedHeaderWhileScrolling() {
        val category = CategoryWithIcon(
            Category(1L, "餐饮", CategoryType.EXPENSE, 1L),
            Icon(1L, "ic_food", "餐饮", "res://"),
        )
        val entries = (1..20).map { index ->
            AnalyticsTransaction(index.toLong(), LocalDate.of(2026, 11, 1), "支出$index", "银行卡", index * 100L,
                category = category, time = LocalTime.of(8, 30).plusMinutes(index - 1L))
        }
        var dismissed = false
        compose.setContent {
            LedgerTheme {
                AnalyticsTransactionSheet("11月支出明细", "2026-11-01 至 2026-11-30", entries, { dismissed = true })
            }
        }
        compose.onNodeWithText("银行卡 · 支出1").assertIsDisplayed()
        compose.onNodeWithText("08:30").assertIsDisplayed()
        compose.onNodeWithText("−¥1.00").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("银行卡 · 支出20"))
        compose.onNodeWithText("银行卡 · 支出20").assertIsDisplayed()
        compose.onNodeWithText("11月支出明细").assertIsDisplayed()
        compose.onNodeWithContentDescription("关闭交易明细").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }
    @Test fun amountOrderKeepsLargestFirstAcrossDatesWithoutDateGroups() {
        val date = LocalDate.of(2026, 11, 1)
        val entries = listOf(
            AnalyticsTransaction(1L, date.plusDays(2), "小额", "现金", 100L),
            AnalyticsTransaction(2L, date, "大额", "现金", 300L),
            AnalyticsTransaction(3L, date.plusDays(1), "中额", "现金", 200L),
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
    }

}
