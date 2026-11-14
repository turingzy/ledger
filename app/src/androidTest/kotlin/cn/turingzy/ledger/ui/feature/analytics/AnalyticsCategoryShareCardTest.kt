package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AnalyticsCategoryShareCardTest {
    @get:Rule val compose = createComposeRule()
    private val expenses = listOf(CategoryExpenseRanking(1L, "餐饮", 120000L, 0.4f), CategoryExpenseRanking(2L, "购物", 180000L, 0.6f))
    private val state = AnalyticsUiState.Success(AnalyticsTimeScope.MONTH, CategoryType.EXPENSE, "", 300000L, 950000L, 650000L,
        expenses, LocalDate.of(2026, 11, 1))

    @Test fun switchingCategoryTypeChangesBarLegendAndKeepsTotalsSeparate() {
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
        compose.onNodeWithText("餐饮 40%").assertIsDisplayed().performClick()
        compose.onNodeWithText("暂无交易记录").assertIsDisplayed()
        compose.onNodeWithContentDescription("关闭交易明细").performClick()
        compose.onNodeWithText("餐饮 40%").assertIsNotSelected()
        compose.onNodeWithText("总支出").assertIsDisplayed()
        compose.onNodeWithText("¥3000.00").assertIsDisplayed()
        compose.onNodeWithText("¥1200.00").assertDoesNotExist()
        compose.onNodeWithText("¥1800.00").assertDoesNotExist()
        val bar = compose.onNodeWithContentDescription("支出分类占比条", substring = true).getUnclippedBoundsInRoot()
        val legend = compose.onNodeWithText("餐饮 40%").getUnclippedBoundsInRoot()
        assertTrue(legend.top >= bar.bottom)
        compose.onNodeWithText("收入").performClick()
        compose.onNodeWithText("总收入").assertIsDisplayed()
        compose.onNodeWithText("工资 100%").assertIsDisplayed()
        compose.onAllNodesWithText("¥9500.00").assertCountEquals(1)
        compose.onNodeWithText("餐饮 40%").assertDoesNotExist()
        compose.onNodeWithText("支出").performClick()
        compose.onNodeWithText("餐饮 40%").assertIsDisplayed().assertIsNotSelected()
    }

    @Test fun allCategoriesAreVisibleWithoutAnInnerScrollingList() {
        val rankings = (1L..10L).map { CategoryExpenseRanking(it, "分类$it", 100L, 0.1f) }
        compose.setContent { LedgerTheme { AnalyticsCategoryShareCard(state.copy(rankings = rankings), {}) } }
        compose.onNodeWithText("分类1 10%").assertIsDisplayed()
        compose.onNodeWithText("分类10 10%").assertIsDisplayed()
    }

    @Test fun compactLegendFitsThreeShortCategoriesBeforeWrapping() {
        val rankings = listOf("餐饮", "购物", "交通", "住房", "娱乐", "日用").mapIndexed { index, name ->
            CategoryExpenseRanking(index.toLong(), name, 100L, 0f)
        }
        compose.setContent { LedgerTheme {
            AnalyticsCategoryShareCard(state.copy(rankings = rankings, totalExpense = 600L), {}, Modifier.width(320.dp))
        } }
        val first = compose.onNodeWithText("餐饮 16.67%").getUnclippedBoundsInRoot()
        val second = compose.onNodeWithText("购物 16.67%").getUnclippedBoundsInRoot()
        val third = compose.onNodeWithText("交通 16.67%").getUnclippedBoundsInRoot()
        val fourth = compose.onNodeWithText("住房 16.67%").getUnclippedBoundsInRoot()
        assertTrue(first.top == second.top && second.top == third.top)
        assertTrue(fourth.top > third.bottom)
    }

    @Test fun legendUsesFixedGapsAndWrapsOnlyWhenTheWholeCategoryWouldOverflow() {
        val names = listOf("餐饮", "日常购物", "交通出行", "住房", "娱乐休闲", "日用")
        val rankings = names.mapIndexed { index, name -> CategoryExpenseRanking(index.toLong(), name, 100L, 0f) }
        compose.setContent { LedgerTheme {
            AnalyticsCategoryShareCard(state.copy(rankings = rankings, totalExpense = 600L), {}, Modifier.width(320.dp))
        } }
        val bar = compose.onNodeWithContentDescription("支出分类占比条", substring = true).getUnclippedBoundsInRoot()
        val bounds = names.map { compose.onNodeWithText("$it 16.67%").getUnclippedBoundsInRoot() }
        assertEquals(bar.left, bounds.first().left)
        bounds.zipWithNext().forEach { (previous, current) ->
            if (current.top == previous.top) {
                assertEquals(16.dp, current.left - previous.right)
                assertTrue(current.right <= bar.right)
            } else {
                assertEquals(bar.left, current.left)
                assertTrue(previous.right + 16.dp + (current.right - current.left) > bar.right)
            }
        }
    }

    @Test fun emptyPeriodKeepsSwitchAndEmptyBarAvailable() {
        compose.setContent { LedgerTheme { AnalyticsCategoryShareCard(state.copy(rankings = emptyList(), totalExpense = 0L), {}) } }
        compose.onNodeWithText("总支出").assertIsDisplayed()
        compose.onNodeWithText("暂无支出记录").assertIsDisplayed()
        compose.onNodeWithText("收入").assertIsDisplayed()
    }
}
