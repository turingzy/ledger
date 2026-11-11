package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsOff
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.util.toFormattedAmount
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
    fun categorySelectorKeepsTextColorsAndWeightsWhenSelectionChanges() {
        val categoryType = mutableStateOf(CategoryType.EXPENSE)
        compose.setContent {
            MaterialTheme {
                CategoryBreakdownCard(sample().copy(categoryType = categoryType.value), { categoryType.value = it })
            }
        }
        val expenseColor = textLayout("支出").layoutInput.style.color
        val incomeColor = textLayout("收入").layoutInput.style.color
        val expenseWeight = textLayout("支出").layoutInput.style.fontWeight
        val incomeWeight = textLayout("收入").layoutInput.style.fontWeight
        assertEquals(expenseColor, incomeColor)
        assertEquals(expenseWeight, incomeWeight)
        compose.onNodeWithText("收入").performClick()
        assertEquals(expenseColor, textLayout("支出").layoutInput.style.color)
        assertEquals(incomeColor, textLayout("收入").layoutInput.style.color)
        assertEquals(expenseWeight, textLayout("支出").layoutInput.style.fontWeight)
        assertEquals(incomeWeight, textLayout("收入").layoutInput.style.fontWeight)
    }

    @Test
    fun comparisonUsesOriginalColumnsForCurrentPreviousAndChange() {
        val state = sample().copy(totalIncome = 31300L, totalExpense = 52200L, balance = -20900L)
        val previous = AnalyticsComparison(first.minusMonths(1), first.minusDays(1), 11000L, 40000L)
        compose.setContent {
            MaterialTheme { AnalyticsComparisonCard(state, previous, Modifier.width(320.dp)) }
        }
        compose.onNodeWithText("本月").assertExists()
        compose.onNodeWithText("上月").assertExists()
        compose.onNodeWithText("增长").assertExists()
        compose.onNodeWithContentDescription("收支对比分组柱状图", substring = true).assertExists()
        val income = compose.onNodeWithText(state.totalIncome.toFormattedAmount(true)).getUnclippedBoundsInRoot()
        val expense = compose.onNodeWithText(state.totalExpense.toFormattedAmount(true)).getUnclippedBoundsInRoot()
        val balance = compose.onNodeWithText(state.balance.toFormattedAmount(true)).getUnclippedBoundsInRoot()
        assertEquals(income.right, expense.right)
        assertEquals(income.right, balance.right)
    }

    @Test
    fun vicoTrendHandlesEmptyDataAndPreservesIndependentSeriesSwitches() {
        val points = mutableStateOf(emptyList<AnalyticsTrendPoint>())
        compose.setContent {
            MaterialTheme {
                TrendLineChartCard(points.value, first, first.plusDays(2), AnalyticsTimeScope.MONTH)
            }
        }
        compose.onNodeWithText("这个周期暂无收支记录").assertExists()
        compose.runOnIdle {
            points.value = listOf(
                AnalyticsTrendPoint(first, 1000L, 500L),
                AnalyticsTrendPoint(first.plusDays(1), 100000L, 0L),
                AnalyticsTrendPoint(first.plusDays(2), 1000L, 5000L),
            )
        }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("收支走势图", substring = true).assertExists()
        listOf("支出", "收入", "净结余").forEach { label ->
            compose.onNodeWithContentDescription(label).assertIsOn().performClick().assertIsOff()
        }
        compose.onNodeWithContentDescription("收入").performClick().assertIsOn()
        compose.onNodeWithContentDescription("支出").assertIsOff()
        compose.onNodeWithContentDescription("净结余").assertIsOff()
    }

    @Test
    fun averageCardUpdatesAmountsWithoutRepeatingTheSelectedDateRange() {
        val state = mutableStateOf(sample())
        compose.setContent { MaterialTheme { AnalyticsAverageCard(state.value) } }
        compose.onNodeWithText("按 29 天计算").assertDoesNotExist()
        compose.onNodeWithText(690L.toFormattedAmount(true)).assertExists()
        compose.onNodeWithContentDescription("日均收支柱状图").assertExists()
        compose.runOnIdle { state.value = state.value.copy(periodEnd = first) }
        compose.onNodeWithText("按 1 天计算").assertDoesNotExist()
        compose.onNodeWithText(20000L.toFormattedAmount(true)).assertExists()
    }

    private fun textLayout(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
            assertTrue(action(results))
        }
        return results.single()
    }

    @Test
    fun expandingDetailsGrowsToHeightCapAndAllowsScrollingToLastEntry() {
        compose.setContent { MaterialTheme { CategoryBreakdownCard(sample(), {}) } }
        val list = compose.onNodeWithContentDescription("分类列表")
        val initialBounds = list.getUnclippedBoundsInRoot()
        val before = initialBounds.bottom - initialBounds.top
        compose.onNodeWithContentDescription("分类占比环形图", substring = true).assertExists()
        compose.onNodeWithContentDescription("展开测试工资明细").performClick()
        compose.onNodeWithContentDescription("收起测试工资明细").assertExists()
        compose.waitForIdle()
        val expandedBounds = list.getUnclippedBoundsInRoot()
        assertTrue(before < 240.dp)
        assertEquals(240.dp, expandedBounds.bottom - expandedBounds.top)
        val category = compose.onNodeWithText("测试工资（100.0%）").getUnclippedBoundsInRoot()
        val firstTransaction = compose.onNodeWithText("测试账户0").getUnclippedBoundsInRoot()
        assertTrue(firstTransaction.top - category.bottom <= 8.dp)
        list.performScrollToNode(hasText("测试账户19"))
        compose.onNodeWithText("测试账户19").assertExists()
    }
}
