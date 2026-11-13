package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalyticsCategoryShareCardTest {
    @get:Rule val compose = createComposeRule()
    private val expenses = listOf(CategoryExpenseRanking(1L, "餐饮", 120000L, 0.4f), CategoryExpenseRanking(2L, "购物", 180000L, 0.6f))
    private val state = AnalyticsUiState.Success(AnalyticsTimeScope.MONTH, CategoryType.EXPENSE, "", 300000L, 950000L, 650000L,
        expenses, LocalDate.of(2026, 11, 1))

    @Test fun switchingCategoryTypeChangesRingLegendAndKeepsTotalsSeparate() {
        val current = mutableStateOf(state)
        compose.setContent {
            LedgerTheme {
                AnalyticsCategoryShareCard(current.value, { type ->
                    current.value = state.copy(categoryType = type, rankings = if (type == CategoryType.INCOME)
                        listOf(CategoryExpenseRanking(3L, "工资", 950000L, 1f)) else expenses)
                })
            }
        }
        compose.onNodeWithText("分类占比").assertIsDisplayed()
        compose.onNodeWithText("支出/收入分类占比").assertDoesNotExist()
        compose.onNodeWithText("餐饮 40%").assertIsDisplayed().performClick().assertIsSelected()
        compose.onNodeWithText("总支出").assertIsDisplayed()
        compose.onNodeWithText("¥3000.00").assertIsDisplayed()
        val first = compose.onNodeWithText("¥1200.00").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("¥1800.00").getUnclippedBoundsInRoot()
        assertEquals(first.right, second.right)
        val ring = compose.onNodeWithContentDescription("支出分类环形图", substring = true).getUnclippedBoundsInRoot()
        val legend = compose.onNodeWithText("餐饮 40%").getUnclippedBoundsInRoot()
        assertTrue(legend.top >= ring.bottom)
        compose.onNodeWithText("收入").performClick()
        compose.onNodeWithText("总收入").assertIsDisplayed()
        compose.onNodeWithText("工资 100%").assertIsDisplayed()
        compose.onAllNodesWithText("¥9500.00").assertCountEquals(2)
        compose.onNodeWithText("餐饮 40%").assertDoesNotExist()
        compose.onNodeWithText("支出").performClick()
        compose.onNodeWithText("餐饮 40%").assertIsDisplayed().assertIsNotSelected()
    }

    @Test fun emptyPeriodKeepsSwitchAndEmptyRingAvailable() {
        compose.setContent { LedgerTheme { AnalyticsCategoryShareCard(state.copy(rankings = emptyList(), totalExpense = 0L), {}) } }
        compose.onNodeWithText("总支出").assertIsDisplayed()
        compose.onNodeWithText("暂无支出记录").assertIsDisplayed()
        compose.onNodeWithText("收入").assertIsDisplayed()
    }
}
