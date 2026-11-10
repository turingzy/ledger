package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import cn.turingzy.ledger.model.CategoryType
import java.time.LocalDate
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AnalyticsInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    private val first = LocalDate.of(2024, 2, 1)
    private fun sample() = AnalyticsUiState.Success(
        timeScope = AnalyticsTimeScope.MONTH,
        categoryType = CategoryType.INCOME,
        periodDisplay = "2024 02.01-02.29",
        totalExpense = 0L,
        totalIncome = 20000L,
        balance = 20000L,
        rankings = listOf(CategoryExpenseRanking(
            categoryId = 1L,
            categoryName = "测试工资",
            totalAmount = 20000L,
            percentage = 1f,
            transactions = List(20) { index -> AnalyticsTransaction(
                id = index.toLong(), date = first.plusDays(index.toLong()),
                note = "虚构记录", accountName = "测试账户$index", amount = 1000L,
            ) },
        )),
        periodStart = first,
        trendPoints = List(29) { index -> AnalyticsTrendPoint(first.plusDays(index.toLong()), 0L, 1000L) },
    )

    @Test
    fun expandingDetailsGrowsToHeightCapAndAllowsScrollingToLastEntry() {
        compose.setContent { MaterialTheme { CategoryBreakdownCard(sample(), {}) } }
        val list = compose.onNodeWithContentDescription("分类列表")
        val initialBounds = list.getUnclippedBoundsInRoot()
        val before = initialBounds.bottom - initialBounds.top
        compose.onNodeWithText("测试工资（100.0%）").performClick()
        compose.waitForIdle()
        val expandedBounds = list.getUnclippedBoundsInRoot()
        assertTrue(before < 240.dp)
        assertEquals(240.dp, expandedBounds.bottom - expandedBounds.top)
        list.performScrollToNode(hasText("测试账户19"))
        compose.onNodeWithText("测试账户19").assertExists()
    }
}
