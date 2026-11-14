package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class AnalyticsChartInteractionTest {
    @get:Rule val compose = createComposeRule()
    private val start = LocalDate.of(2026, 11, 1)

    @Test
    fun barIsReadOnlyAndCategoryListOpensMatchingTransactions() {
        val transaction = AnalyticsTransaction(1L, start, "午餐", "现金", 1200L)
        val state = AnalyticsUiState.Success(
            AnalyticsTimeScope.MONTH, CategoryType.EXPENSE, "", 3000L, 0L, -3000L,
            listOf(CategoryExpenseRanking(1L, "餐饮", 1200L, 0.4f, listOf(transaction)),
                CategoryExpenseRanking(2L, "购物", 1800L, 0.6f)), start,
        )
        compose.setContent { LedgerTheme { AnalyticsCategoryShareCard(state, {}) } }
        compose.onNodeWithContentDescription("支出分类占比条", substring = true).performTouchInput {
            click(Offset(width * 0.20f, center.y))
        }
        compose.onNodeWithText("午餐").assertDoesNotExist()
        compose.onNodeWithText("餐饮 40%").assertIsNotSelected()
        compose.onNodeWithContentDescription("支出分类占比条", substring = true).performTouchInput {
            click(Offset(width * 0.20f, center.y))
        }
        compose.onNodeWithText("午餐").assertDoesNotExist()
        compose.onNodeWithContentDescription("支出分类占比条", substring = true).assertHasNoClickAction()
        compose.onNodeWithText("餐饮 40%").assertIsNotSelected().performClick()
        compose.onNodeWithText("午餐").assertIsDisplayed()
        compose.onNodeWithText("−¥12.00").assertIsDisplayed()
        compose.onNodeWithContentDescription("关闭交易明细").performClick()
        compose.onNodeWithText("餐饮 40%").assertIsNotSelected()
    }

    @Test
    fun tappingAndScrubbingUpdateInlineComparisonWithoutOpeningDetails() {
        val end = start.plusDays(29)
        val days = mapOf(start to AnalyticsDay(10000L), end to AnalyticsDay(200000L),
            start.minusDays(2) to AnalyticsDay(125000L))
        compose.setContent {
            LedgerTheme {
                ExpensePace(days, AnalyticsTimeScope.MONTH, start, end, end)
            }
        }
        compose.onNodeWithContentDescription("累计支出对比", substring = true).performTouchInput {
            click(Offset(0f, center.y))
        }
        compose.onNodeWithText("本期 ¥100.00").assertIsDisplayed()
        compose.onNodeWithText("暂无环比").assertDoesNotExist()
        compose.onNodeWithContentDescription("累计支出对比", substring = true).performTouchInput {
            down(center)
            advanceEventTime(700)
            moveTo(Offset(width - 1f, center.y), delayMillis = 200)
            up()
        }
        compose.onNodeWithText("本期 ¥2100.00").assertIsDisplayed()
        compose.onNodeWithText("上期 ¥1250.00").assertIsDisplayed()
        compose.onNodeWithText("68%").assertIsDisplayed().assertHasNoClickAction()
            .performTouchInput { click(center) }
        compose.onNodeWithContentDescription("关闭交易明细").assertDoesNotExist()
        compose.onNodeWithText("大额支出").assertDoesNotExist()
    }
}
