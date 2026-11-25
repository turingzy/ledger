package cn.turingzy.ledger.ui.feature.analytics.calendar

import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsContent
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsDashboardState
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTimeScope
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsTransaction
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsUiState
import cn.turingzy.ledger.ui.feature.analytics.calculation.AnalyticsDay
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.material3.MaterialTheme
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.ui.text.font.FontWeight
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
                AnalyticsTransaction(1L, day, "午餐", 12345L, category = category, time = LocalTime.NOON),
                AnalyticsTransaction(2L, day, "工资", 99900L, TransactionType.INCOME),
            ),
        )
        var openedTransaction: Long? = null
        compose.setContent {
            LedgerTheme {
                ConsumptionCalendarContent(
                    dashboard, YearMonth.from(today), {}, today = today,
                    onTransactionClick = { openedTransaction = it },
                )
            }
        }
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
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("12:00 · 午餐"))
        compose.onNodeWithText("12:00 · 午餐").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1L, openedTransaction) }
        compose.onNodeWithText("−¥123.45").assertIsDisplayed()
        compose.onNodeWithText("工资").assertDoesNotExist()
    }

    @Test fun monthPickerAndCloseWorkWithoutArrowsOrTodayButton() {
        var back = false
        compose.setContent { LedgerTheme {
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = false), YearMonth.of(2024, 2), { back = true }, today = today)
        } }
        compose.onNodeWithContentDescription("2024-02-29，支出¥0.00").assertExists()
        compose.onNodeWithContentDescription("下个月").assertDoesNotExist()
        compose.onNodeWithContentDescription("上个月").assertDoesNotExist()
        compose.onNodeWithText("今天").assertDoesNotExist()
        compose.onNode(hasText("2024.02") and hasContentDescription("选择月份")).performClick()
        compose.onNodeWithTag("ledgerPeriodPicker").assertIsDisplayed()
        compose.onNodeWithTag("ledgerPeriod-${LocalDate.of(2024, 3, 1).toEpochDay()}").performClick()
        compose.onNodeWithText("2024.03").assertIsDisplayed()
        compose.onNodeWithContentDescription("关闭消费日历").performClick()
        compose.runOnIdle { assertTrue(back) }
    }

    @Test fun spendingButtonOpensTheCalendarSheetForTheSelectedPeriod() {
        val start = LocalDate.of(2024, 1, 1)
        val state = AnalyticsUiState.Success(AnalyticsTimeScope.YEAR, CategoryType.EXPENSE, "2024年",
            0L, 0L, 0L, emptyList(), start)
        compose.setContent { LedgerTheme {
            var anchor by remember { mutableStateOf<LocalDate?>(null) }
            AnalyticsContent(state, {}, {}, onOpenCalendar = { anchor = it })
            anchor?.let { date ->
                ConsumptionCalendarContent(
                    AnalyticsDashboardState(loading = false),
                    YearMonth.from(date),
                    onDismiss = { anchor = null },
                    today = today,
                )
            }
        } }
        compose.onNodeWithText("消费").assertHasClickAction().performClick()
        compose.onNode(isDialog()).assertExists()
        compose.onNodeWithText("2024.12").assertIsDisplayed()
        compose.onNodeWithContentDescription("关闭消费日历").performClick()
        compose.onNode(isDialog()).assertDoesNotExist()
        compose.onNodeWithText("消费").assertIsDisplayed()
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
        val list = compose.onNodeWithTag("consumptionCalendarList")
        list.performScrollToNode(hasText("这一天没有支出"))
        compose.onNodeWithText("这一天没有支出").assertIsDisplayed()
        list.performScrollToNode(hasText("记一笔"))
        compose.onNodeWithText("记一笔").performClick()
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
        compose.onNodeWithText("缺少汇率。请前往「设置 → 常规 → 汇率」刷新或手动填写。").assertIsDisplayed()
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
        assertEquals(18.sp, header.single().layoutInput.style.fontSize)
        assertEquals(14.sp, count.single().layoutInput.style.fontSize)
    }

    @Test fun monthPillMatchesBillTypographyAndHeight() {
        var foreground = Color.Unspecified
        compose.setContent { LedgerTheme {
            foreground = MaterialTheme.colorScheme.onSurface
            ConsumptionCalendarContent(AnalyticsDashboardState(loading = false), YearMonth.from(today), {}, today = today)
        } }
        val month = mutableListOf<TextLayoutResult>()
        val amount = mutableListOf<TextLayoutResult>()
        val label = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("2026.11").assertHeightIsEqualTo(32.dp).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(month) }
        compose.onNodeWithText("11月总支出").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(label) }
        compose.onAllNodesWithText("¥0.00")[0].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(amount) }
        assertEquals(17.sp, month.single().layoutInput.style.fontSize)
        assertEquals(FontWeight.Medium, month.single().layoutInput.style.fontWeight)
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
        compose.onNodeWithTag("consumptionCalendarList").performScrollToNode(hasText("记一笔"))
        val action = compose.onNodeWithText("记一笔")
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
        val list = compose.onNodeWithTag("consumptionCalendarList")
        list.performScrollToNode(hasText("这一天没有支出"))
        compose.onNodeWithText("这一天没有支出").assertIsDisplayed()
        list.performScrollToNode(hasText("记一笔"))
        compose.onNodeWithText("记一笔").performClick()
        compose.runOnIdle { assertEquals(month.atDay(1), recordDate) }
    }

}
