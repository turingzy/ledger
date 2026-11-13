package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.turingzy.ledger.model.CategoryType
import java.time.LocalDate
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AnalyticsControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun allScopeShowsFullRangeWithoutNavigationArrows() {
        val start = LocalDate.of(2021, 12, 31)
        compose.setContent {
            MaterialTheme {
                AnalyticsContent(
                    AnalyticsUiState.Success(
                        timeScope = AnalyticsTimeScope.ALL, categoryType = CategoryType.EXPENSE,
                        periodDisplay = "2021-12-31 - 2026-02-03", totalExpense = 0L, totalIncome = 0L,
                        balance = 0L, rankings = emptyList(), periodStart = start,
                        periodEnd = LocalDate.of(2026, 2, 3),
                    ), {}, {},
                )
            }
        }
        compose.onNodeWithText("3个月").assertDoesNotExist()
        compose.onNodeWithText("全部").assertIsDisplayed()
        compose.onNodeWithText("2021 12.31").assertIsDisplayed()
        compose.onNodeWithText("2026 02.03").assertIsDisplayed()
        compose.onNodeWithText("～").assertIsDisplayed()
        val startBounds = compose.onNodeWithContentDescription("选择分析开始日期").getUnclippedBoundsInRoot()
        val endBounds = compose.onNodeWithContentDescription("选择分析结束日期").getUnclippedBoundsInRoot()
        val separator = compose.onNodeWithText("～").getUnclippedBoundsInRoot()
        assertTrue(startBounds.left > 16.dp)
        assertEquals(16.dp, separator.left - startBounds.right)
        assertEquals(16.dp, endBounds.left - separator.right)
        compose.onNodeWithContentDescription("管理分析卡片").assertDoesNotExist()
        compose.onNodeWithContentDescription("日历").assertIsDisplayed().assertHasNoClickAction()
        listOf("收支对比", "收支走势", "分类占比", "日均收支").forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
        compose.onNodeWithContentDescription("上一周期").assertDoesNotExist()
        compose.onNodeWithContentDescription("下一周期").assertDoesNotExist()
    }

    @Test
    fun monthDateOpensSharedDrawerWithoutAnInternalViewSelector() {
        assertPeriodDrawerSelection(AnalyticsTimeScope.MONTH, LocalDate.of(2024, 3, 1))
    }

    @Test
    fun weekDateOpensSharedDrawerAndSelectsAWholeWeek() {
        assertPeriodDrawerSelection(AnalyticsTimeScope.WEEK, LocalDate.of(2024, 2, 12))
    }

    @Test
    fun yearDateOpensSharedDrawerAndSelectsAWholeYear() {
        assertPeriodDrawerSelection(AnalyticsTimeScope.YEAR, LocalDate.of(2025, 1, 1))
    }

    private fun assertPeriodDrawerSelection(scope: AnalyticsTimeScope, target: LocalDate) {
        var selected: LocalDate? = null
        compose.setContent {
            MaterialTheme {
                AnalyticsContent(
                    AnalyticsUiState.Success(
                        timeScope = scope, categoryType = CategoryType.EXPENSE,
                        periodDisplay = "2024.02", totalExpense = 0L, totalIncome = 0L,
                        balance = 0L, rankings = emptyList(), periodStart = LocalDate.of(2024, 2, 1),
                    ), {}, {}, onSelectPeriod = { selected = it },
                )
            }
        }
        compose.onNodeWithContentDescription("选择分析日期").performClick()
        compose.onNodeWithTag("ledgerPeriodPicker").assertIsDisplayed()
        compose.onNodeWithTag("periodViewSelector").assertDoesNotExist()
        assertNull(selected)
        compose.onNodeWithTag("ledgerPeriod-${target.toEpochDay()}").performClick()
        assertEquals(target, selected)
    }

    @Test
    fun bothRangeDatesUseDateOnlyPickerAndApplyOnlyAfterConfirmation() {
        val start = LocalDate.of(2026, 11, 12)
        val end = LocalDate.of(2026, 11, 14)
        val state = mutableStateOf(AnalyticsUiState.Success(
            timeScope = AnalyticsTimeScope.ALL, categoryType = CategoryType.EXPENSE,
            periodDisplay = "", totalExpense = 0L, totalIncome = 0L, balance = 0L,
            rankings = emptyList(), periodStart = start, periodEnd = end,
        ))
        compose.setContent {
            MaterialTheme {
                AnalyticsContent(state.value, {}, {}, onSelectRange = { from, to ->
                    state.value = state.value.copy(periodStart = from, periodEnd = to)
                })
            }
        }
        compose.onNodeWithContentDescription("选择分析开始日期").performClick()
        compose.onNodeWithTag("ledgerDatePickerDialog").assertIsDisplayed()
        compose.onNodeWithContentDescription("小时").assertDoesNotExist()
        compose.onNodeWithContentDescription("分钟").assertDoesNotExist()
        compose.onNodeWithContentDescription("2026-11-11").performClick()
        compose.onNodeWithContentDescription("取消").performClick()
        assertEquals(start, state.value.periodStart)
        compose.onNodeWithContentDescription("选择分析开始日期").performClick()
        compose.onNodeWithContentDescription("2026-11-11").performClick()
        compose.onNodeWithContentDescription("确认").performClick()
        assertEquals(start.minusDays(1), state.value.periodStart)
        compose.onNodeWithContentDescription("选择分析结束日期").performClick()
        compose.onNodeWithContentDescription("2026-11-15").performClick()
        compose.onNodeWithContentDescription("确认").performClick()
        assertEquals(end.plusDays(1), state.value.periodEnd)
        assertEquals(AnalyticsTimeScope.ALL, state.value.timeScope)
    }

    @Test
    fun crossedRangeEndpointsKeepAValidSingleDayRange() {
        var selected: Pair<LocalDate, LocalDate>? = null
        val date = LocalDate.of(2026, 11, 12)
        compose.setContent {
            MaterialTheme {
                AnalyticsContent(
                    AnalyticsUiState.Success(
                        timeScope = AnalyticsTimeScope.ALL, categoryType = CategoryType.EXPENSE,
                        periodDisplay = "", totalExpense = 0L, totalIncome = 0L, balance = 0L,
                        rankings = emptyList(), periodStart = date, periodEnd = date.plusDays(2),
                    ), {}, {}, onSelectRange = { start, end -> selected = start to end },
                )
            }
        }
        compose.onNodeWithContentDescription("选择分析结束日期").performClick()
        compose.onNodeWithContentDescription("2026-11-11").performClick()
        compose.onNodeWithContentDescription("确认").performClick()
        assertEquals(date.minusDays(1) to date.minusDays(1), selected)
    }
}
