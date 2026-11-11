package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import cn.turingzy.ledger.model.AnalyticsCard
import cn.turingzy.ledger.model.AnalyticsCardSettings
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
                    ), {}, {}, {},
                )
            }
        }
        compose.onNodeWithText("3个月").assertDoesNotExist()
        compose.onNodeWithText("全部").assertIsDisplayed()
        compose.onNodeWithText("2021-12-31 - 2026-02-03").assertIsDisplayed()
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
                    ), {}, {}, {}, onSelectPeriod = { selected = it },
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
    fun cardDragStaysWithinRowsAndPersistsOrder() {
        val settings = mutableStateOf(AnalyticsCardSettings())
        compose.setContent {
            MaterialTheme {
                AnalyticsCardMenu(settings.value, { _, _ -> }, { settings.value = settings.value.withOrder(it) })
            }
        }
        compose.onNodeWithContentDescription("管理分析卡片").performClick()
        compose.onNodeWithText("卡片管理").assertDoesNotExist()
        val firstCard = settings.value.order.first()
        val title = when (firstCard) {
            AnalyticsCard.COMPARISON -> "收支对比"
            AnalyticsCard.TREND -> "收支走势"
            AnalyticsCard.CATEGORIES -> "分类占比"
            AnalyticsCard.AVERAGES -> "日均收支"
        }
        val card = compose.onNodeWithTag("analyticsCard_${firstCard.name}")
        val second = compose.onNodeWithTag("analyticsCard_${settings.value.order[1].name}")
        assertEquals(2.dp, second.getUnclippedBoundsInRoot().top - card.getUnclippedBoundsInRoot().bottom)
        val handle = compose.onNodeWithContentDescription("拖动${title}排序")
        val rows = compose.onNodeWithTag("analyticsCardRows").getUnclippedBoundsInRoot()
        val top = handle.getUnclippedBoundsInRoot().top
        handle.performTouchInput {
            down(center)
            repeat(8) { moveBy(Offset(0f, -8f)) }
            moveBy(Offset(0f, -500f))
        }
        assertEquals(top, handle.getUnclippedBoundsInRoot().top)
        handle.performTouchInput { moveBy(Offset(0f, 1000f)) }
        val bottomCard = card.getUnclippedBoundsInRoot()
        assertTrue(bottomCard.top >= rows.top)
        assertTrue(bottomCard.bottom <= rows.bottom)
        handle.performTouchInput { up() }
        compose.waitForIdle()
        assertEquals(firstCard, settings.value.order.last())
    }
}
