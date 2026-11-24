package cn.turingzy.ledger.ui.feature.bill

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cn.turingzy.ledger.model.transaction.DateFilterType
import cn.turingzy.ledger.ui.feature.bill.search.BillSearchHeader
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.ui.interaction.LocalUiInteractionClock
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BillMigrationTest {
    @get:Rule val compose = createComposeRule()
    private val state = BillUiState.Success(2026, 11, "2026.11", "11月结余", 0L, 0L, 0L, emptyList())

    @Test fun billSearchButtonNavigatesOutOfTheBillHeader() {
        var opened = false
        compose.setContent { LedgerTheme { BillContent(state, onOpenSearch = { opened = true }) } }
        compose.onNodeWithContentDescription("搜索").performClick()
        compose.runOnIdle { assertTrue(opened) }
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
    }

    @Test fun searchFieldContainsItsCloseButtonAndSubmitsOnIme() {
        var submitted = ""
        var closed = false
        compose.setContent { LedgerTheme {
            var input by remember { mutableStateOf("") }
            BillSearchHeader(input, { input = it }, { submitted = input }, { closed = true })
        } }
        val field = compose.onNode(hasSetTextAction())
        val close = compose.onNodeWithContentDescription("关闭搜索")
        compose.onNodeWithContentDescription("返回").assertDoesNotExist()
        val insideField = hasAnyAncestor(hasTestTag("billSearchField"))
        compose.onNode(hasContentDescription("关闭搜索") and insideField, useUnmergedTree = true)
            .assertWidthIsEqualTo(LedgerStyle.icons.default)
        assertTrue(field.fetchSemanticsNode().boundsInRoot.right < close.fetchSemanticsNode().boundsInRoot.left)
        field.performTextInput("午餐")
        field.performImeAction()
        compose.runOnIdle { assertEquals("午餐", submitted) }
        close.performClick()
        compose.runOnIdle { assertTrue(closed) }
    }

    @Test fun dateEntryUsesThemeTextWhileKeepingSeventeenSp() {
        var foreground = Color.Unspecified
        compose.setContent { LedgerTheme(darkTheme = true) {
            foreground = LedgerStyle.colors.textPrimary
            BillContent(state)
        } }
        compose.onNodeWithContentDescription("选择账本日期").assertHeightIsEqualTo(32.dp)
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("2026.11", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(17.sp, results.single().layoutInput.style.fontSize)
        assertEquals(FontWeight.Medium, results.single().layoutInput.style.fontWeight)
        assertEquals(foreground, results.single().layoutInput.style.color)
    }

    @Test fun crossYearWeekFitsInTheDateEntryOnANarrowScreen() {
        val weekLabel = "2026 12.27\n2027 01.02"
        compose.setContent {
            LedgerTheme {
                BillContent(
                    state.copy(
                        filterType = DateFilterType.WEEK,
                        monthDisplay = "2026 12.27-2027 01.02",
                        filterStartDate = LocalDate.of(2026, 12, 27),
                        filterEndDate = LocalDate.of(2027, 1, 2),
                    ),
                    modifier = Modifier.width(320.dp),
                )
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(weekLabel, useUnmergedTree = true)
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        assertTrue(!layout.hasVisualOverflow)
        assertEquals(2, layout.lineCount)
        assertEquals(12.sp, layout.layoutInput.style.fontSize)
        assertEquals(weekLabel.length, layout.getLineEnd(layout.lineCount - 1, visibleEnd = true))
    }

    @Test fun sameYearWeekStaysOnOneLineInTheDateEntry() {
        val weekLabel = "2026 11.22-11.28"
        compose.setContent {
            LedgerTheme {
                BillContent(
                    state.copy(
                        filterType = DateFilterType.WEEK,
                        monthDisplay = weekLabel,
                        filterStartDate = LocalDate.of(2026, 11, 22),
                        filterEndDate = LocalDate.of(2026, 11, 28),
                    ),
                    modifier = Modifier.width(320.dp),
                )
            }
        }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(weekLabel, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        val layout = results.single()
        assertTrue(!layout.hasVisualOverflow)
        assertEquals(1, layout.lineCount)
        assertEquals(17.sp, layout.layoutInput.style.fontSize)
        assertEquals(weekLabel.length, layout.getLineEnd(0, visibleEnd = true))
    }

    @Test fun transactionClickUsesTheSharedGateAndPreservesTheSelectedId() {
        val case = BillPreviewProvider().values.first { (it.state as? BillUiState.Success)?.dayGroups?.isNotEmpty() == true }
        val populated = case.state as BillUiState.Success
        val clicks = mutableListOf<Long>()
        var now = 0L
        compose.setContent {
            CompositionLocalProvider(LocalUiInteractionClock provides { now }) {
                LedgerTheme { BillContent(populated, onTransactionClick = { clicks += it }) }
            }
        }
        compose.onNodeWithText("餐饮").performClick().performClick()
        compose.runOnIdle { assertEquals(listOf(1L), clicks); now = 400L }
        compose.onNodeWithText("餐饮").performClick()
        compose.runOnIdle { assertEquals(listOf(1L, 1L), clicks) }
    }
}
