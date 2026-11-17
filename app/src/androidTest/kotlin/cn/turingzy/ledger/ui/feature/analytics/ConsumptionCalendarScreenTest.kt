package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.material3.MaterialTheme
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.ui.text.font.FontWeight
import cn.turingzy.ledger.model.*
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ConsumptionCalendarScreenTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 14)

    @Test fun selectingDayDisplaysOnlyItsExpensesUsingLedgerRows() {
        val day = today.minusDays(4)
        val category = CategoryWithIcon(Category(1L, "餐饮", CategoryType.EXPENSE, 1L), Icon(1L, "ic_food", "餐饮", "res://"))
        val dashboard = AnalyticsDashboardState(
            days = mapOf(day to AnalyticsDay(12345L, 99900L)), loading = false,
            transactions = listOf(
                AnalyticsTransaction(1L, day, "午餐", "银行卡", 12345L, category = category, time = LocalTime.NOON),
                AnalyticsTransaction(2L, day, "工资", "银行卡", 99900L, TransactionType.INCOME),
            ),
        )
        compose.setContent { LedgerTheme { ConsumptionCalendarContent(dashboard, YearMonth.from(today), {}, today = today) } }
        compose.onNodeWithText("消费日历").assertIsDisplayed()
        compose.onNodeWithText("11月总支出").assertIsDisplayed()
        compose.onNodeWithText("¥123.45").assertIsDisplayed()
        compose.onNodeWithText("消费天数").assertDoesNotExist()
        compose.onNodeWithText("单日最高").assertDoesNotExist()
        compose.onNodeWithContentDescription("$day，支出¥123.45").performClick().assertIsSelected()
        compose.onNodeWithText("六").assertIsSelected()
        compose.onNodeWithText("二").assertIsNotSelected()
        compose.onNodeWithText("11月10日 共1笔消费").assertExists()
        compose.onNodeWithContentDescription("${today.plusDays(1)}，支出¥0.00").assertIsNotEnabled()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("银行卡 · 午餐"))
        compose.onNodeWithText("银行卡 · 午餐").assertIsDisplayed()
        compose.onNodeWithText("−¥123.45").assertIsDisplayed()
        compose.onNodeWithText("12:00").assertIsDisplayed()
        compose.onNodeWithText("工资").assertDoesNotExist()
    }

    @Test fun monthPickerAndBackWorkWithoutArrowsOrTodayButton() {
        var back = false
        compose.setContent { LedgerTheme {
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = false), YearMonth.of(2024, 2), { back = true }, today = today)
        } }
        compose.onNodeWithContentDescription("2024-02-29，支出¥0.00").assertExists()
        compose.onNodeWithContentDescription("下个月").assertDoesNotExist()
        compose.onNodeWithContentDescription("上个月").assertDoesNotExist()
        compose.onNodeWithText("今天").assertDoesNotExist()
        compose.onNodeWithText("2024年2月").performClick()
        compose.onNodeWithTag("ledgerPeriodPicker").assertIsDisplayed()
        compose.onNodeWithTag("ledgerPeriod-${LocalDate.of(2024, 3, 1).toEpochDay()}").performClick()
        compose.onNodeWithText("2024年3月").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回分析").performClick()
        compose.runOnIdle { assertTrue(back) }
    }

    @Test fun analyticsTopRightCalendarEntryPassesTheSelectedPeriod() {
        val start = LocalDate.of(2024, 2, 1)
        val state = AnalyticsUiState.Success(AnalyticsTimeScope.MONTH, CategoryType.EXPENSE, "2024年2月",
            0L, 0L, 0L, emptyList(), start)
        var anchor: LocalDate? = null
        compose.setContent { LedgerTheme {
            AnalyticsContent(state, {}, {}, onOpenCalendar = { anchor = it })
        } }
        compose.onNodeWithContentDescription("消费日历热力图").assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(LocalDate.of(2024, 2, 29), anchor) }
    }

    @Test fun emptyDayOffersRecordingForTheSelectedDateAndKeepsTheHeaderFixed() {
        val day = today.minusDays(3)
        var recordDate: LocalDate? = null
        compose.setContent { LedgerTheme {
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = false), YearMonth.from(today), {},
                today = today, onRecordTransaction = { recordDate = it })
        } }
        compose.onNodeWithContentDescription("$day，支出¥0.00").performClick()
        val title = compose.onNodeWithText("消费日历").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("这一天没有支出").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("记一笔").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(day, recordDate) }
        assertEquals(title, compose.onNodeWithText("消费日历").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun loadingDoesNotOfferAFalseEmptyStateOrRecordButton() {
        compose.setContent { LedgerTheme {
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = true), YearMonth.from(today), {}, today = today)
        } }
        compose.onNodeWithText("这一天没有支出").assertDoesNotExist()
        compose.onNodeWithText("记一笔").assertDoesNotExist()
    }

    @Test fun missingRatesDoNotExposeAZeroTotalOrRecordAction() {
        compose.setContent { LedgerTheme {
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = false, amountsAvailable = false),
                YearMonth.from(today), {}, today = today)
        } }
        compose.onNodeWithText("缺少汇率，请在常规中刷新或填写").assertIsDisplayed()
        compose.onNodeWithText("记一笔").assertDoesNotExist()
        compose.onNodeWithContentDescription("$today，支出¥0.00").assertDoesNotExist()
    }

    @Test fun onlyANewSelectableDayProducesSelectionHaptics() {
        val events = mutableListOf<HapticFeedbackType>()
        val feedback = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { events.add(hapticFeedbackType) }
        }
        compose.setContent {
            CompositionLocalProvider(LocalHapticFeedback provides feedback) {
                LedgerTheme {
                    ConsumptionCalendarContent(AnalyticsDashboardState(loading = false), YearMonth.from(today), {}, today = today)
                }
            }
        }
        compose.onNodeWithContentDescription("$today，支出¥0.00").performClick()
        compose.onNodeWithContentDescription("${today.plusDays(1)}，支出¥0.00").performTouchInput { click() }
        compose.runOnIdle { assertTrue(events.isEmpty()) }
        val day = today.minusDays(1)
        compose.onNodeWithContentDescription("$day，支出¥0.00").performClick().assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(listOf(HapticFeedbackType.TextHandleMove), events) }
    }

    @Test fun dayCountKeepsSecondaryTypographyBelowThePageTitle() {
        compose.setContent { LedgerTheme {
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = false), YearMonth.from(today), {}, today = today)
        } }
        val header = mutableListOf<TextLayoutResult>()
        val count = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("消费日历").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(header) }
        compose.onNodeWithText("11月14日 共0笔消费").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(count) }
        assertEquals(17.sp, header.single().layoutInput.style.fontSize)
        assertEquals(14.sp, count.single().layoutInput.style.fontSize)
    }

    @Test fun monthHeadingKeepsItsOriginalNeutralColorAndTypography() {
        var foreground = Color.Unspecified
        compose.setContent { LedgerTheme {
            foreground = MaterialTheme.colorScheme.onSurface
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = false), YearMonth.from(today), {}, today = today)
        } }
        val month = mutableListOf<TextLayoutResult>()
        val amount = mutableListOf<TextLayoutResult>()
        val label = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("2026年11月").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(month) }
        compose.onNodeWithText("11月总支出").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(label) }
        compose.onAllNodesWithText("¥0.00")[0].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(amount) }
        assertEquals(16.sp, month.single().layoutInput.style.fontSize)
        assertEquals(FontWeight.SemiBold, month.single().layoutInput.style.fontWeight)
        assertEquals(foreground, month.single().layoutInput.style.color)
        assertEquals(11.sp, label.single().layoutInput.style.fontSize)
        assertEquals(15.sp, amount.single().layoutInput.style.fontSize)
        assertEquals(FontWeight.SemiBold, amount.single().layoutInput.style.fontWeight)
    }

    @Test fun recordActionKeepsANeutralBackgroundAndRightHandIcon() {
        var background = Color.Unspecified
        var foreground = Color.Unspecified
        compose.setContent { LedgerTheme {
            background = LedgerStyle.colors.background
            foreground = LedgerStyle.colors.textSecondary
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = false), YearMonth.from(today), {}, today = today)
        } }
        val action = compose.onNodeWithText("记一笔").performScrollTo()
        val pixels = action.captureToImage().toPixelMap()
        assertEquals(background, pixels[pixels.width / 2, 2])
        val text = compose.onNodeWithText("记一笔", useUnmergedTree = true)
        val icon = compose.onNodeWithTag("baseButtonTrailingIcon", useUnmergedTree = true)
        assertTrue(icon.fetchSemanticsNode().boundsInRoot.left >= text.fetchSemanticsNode().boundsInRoot.right)
        val layout = mutableListOf<TextLayoutResult>()
        text.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layout) }
        assertEquals(foreground, layout.single().layoutInput.style.color)
    }

    @Test fun aFutureMonthReusesTheSameEmptyStateAndRecordAction() {
        val month = YearMonth.from(today).plusMonths(1)
        var recordDate: LocalDate? = null
        compose.setContent { LedgerTheme {
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = false), month, {}, today = today,
                onRecordTransaction = { recordDate = it })
        } }
        compose.onNodeWithText("尚未到来").assertDoesNotExist()
        compose.onNodeWithText("这一天没有支出").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("记一笔").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(month.atDay(1), recordDate) }
    }

}
