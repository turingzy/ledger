package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.ui.component.weekStart
import cn.turingzy.ledger.ui.component.periodStart
import cn.turingzy.ledger.ui.component.PeriodSeries
import cn.turingzy.ledger.ui.component.PeriodPickerSheet
import cn.turingzy.ledger.model.DateFilterType
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performClick
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PeriodPickerSheetTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun weekListScrollsWithoutApplyingAndClickSelectsWholeWeek() {
        assertScrollingSelection(DateFilterType.WEEK, LocalDate.of(2025, 1, 1))
    }

    @Test
    fun monthGridScrollsByYearAndClickSelectsCompleteMonth() {
        assertScrollingSelection(DateFilterType.MONTH, LocalDate.of(2025, 3, 1))
    }

    @Test
    fun yearListScrollsAndClickSelectsCompleteYear() {
        assertScrollingSelection(DateFilterType.YEAR, LocalDate.of(2025, 1, 1))
    }

    private fun assertScrollingSelection(type: DateFilterType, target: LocalDate) {
        val initial = LocalDate.of(2024, 2, 29)
        var selected: Pair<DateFilterType, LocalDate>? = null
        compose.setContent {
            MaterialTheme {
                PeriodPickerSheet(type, initial, {}, { view, date -> selected = view to date })
            }
        }
        val listType = if (type == DateFilterType.MONTH) DateFilterType.YEAR else type
        val series = PeriodSeries(listType, initial)
        compose.onNodeWithTag("ledgerPeriodList").performScrollToIndex(series.indexOf(target))
        assertNull(selected)
        val normalized = periodStart(type, target)
        compose.onNodeWithTag("ledgerPeriod-${normalized.toEpochDay()}").performClick()
        assertEquals(type to normalized, selected)
    }

    @Test
    fun switchingViewStaysInsidePickerWithoutApplyingUntilAnItemIsClicked() {
        var selected: Pair<DateFilterType, LocalDate>? = null
        compose.setContent {
            MaterialTheme {
                PeriodPickerSheet(DateFilterType.MONTH, LocalDate.of(2024, 2, 1), {}, { type, date -> selected = type to date })
            }
        }
        compose.onNodeWithText("年").performClick()
        compose.mainClock.advanceTimeBy(240)
        compose.waitForIdle()
        compose.onNodeWithContentDescription("确认日期选择").assertDoesNotExist()
        val year = LocalDate.of(2024, 1, 1)
        compose.onNodeWithTag("ledgerPeriod-${year.toEpochDay()}").assertIsSelected()
        assertNull(selected)
        compose.onNodeWithTag("ledgerPeriod-${year.toEpochDay()}").performClick()
        assertEquals(DateFilterType.YEAR to year, selected)
    }

    @Test
    fun returnShortcutUsesTheLocallySelectedView() {
        var selected: Pair<DateFilterType, LocalDate>? = null
        compose.setContent {
            MaterialTheme {
                PeriodPickerSheet(DateFilterType.MONTH, LocalDate.of(2024, 1, 1), {}, { type, date -> selected = type to date })
            }
        }
        compose.onNodeWithText("周").performClick()
        compose.mainClock.advanceTimeBy(240)
        compose.waitForIdle()
        compose.onNodeWithContentDescription("返回本周").performClick()
        assertEquals(DateFilterType.WEEK to weekStart(LocalDate.now()), selected)
    }

    @Test
    fun currentPeriodKeepsDrawerUndoVisibleAndDisabledWhileBrowsingOtherItems() {
        val today = LocalDate.now()
        compose.setContent {
            MaterialTheme {
                PeriodPickerSheet(DateFilterType.MONTH, today, {}, { _, _ -> })
            }
        }
        compose.onNodeWithContentDescription("返回本月").assertIsDisplayed().assertIsNotEnabled()
        val series = PeriodSeries(DateFilterType.YEAR, today)
        compose.onNodeWithTag("ledgerPeriodList").performScrollToIndex(series.indexOf(today.minusYears(2)))
        compose.onNodeWithContentDescription("返回本月").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun ledgerDateIsTheOnlyEntryAndCancelKeepsCurrentView() {
        var selected: Pair<DateFilterType, LocalDate>? = null
        compose.setContent {
            MaterialTheme {
                LedgerContent(
                    LedgerUiState.Success(
                        year = 2024, month = 2, monthDisplay = "2024.02", cardMonthDisplay = "结余",
                        monthIncome = 0L, monthExpense = 0L, monthBalance = 0L, dayGroups = emptyList(),
                        filterType = DateFilterType.MONTH, filterStartDate = LocalDate.of(2024, 2, 1),
                    ),
                    onSelectPeriod = { type, date -> selected = type to date },
                )
            }
        }
        compose.onNodeWithContentDescription("切换账本视图").assertDoesNotExist()
        compose.onNodeWithText("本月").assertIsDisplayed()
        compose.onNodeWithContentDescription("选择账本日期").performClick()
        compose.onNodeWithTag("ledgerPeriodList").assertIsDisplayed()
        compose.onNodeWithText("周").performClick()
        compose.mainClock.advanceTimeBy(240)
        compose.waitForIdle()
        compose.onNodeWithText("周一").assertIsDisplayed()
        compose.onNodeWithContentDescription("关闭日期选择").performClick()
        compose.onNodeWithTag("ledgerPeriodPicker").assertDoesNotExist()
        compose.onNodeWithText("2024.02").assertIsDisplayed()
        assertNull(selected)
    }
}
