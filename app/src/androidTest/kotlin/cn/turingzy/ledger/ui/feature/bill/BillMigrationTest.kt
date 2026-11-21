package cn.turingzy.ledger.ui.feature.bill

import androidx.compose.runtime.*
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

    @Test fun searchSubpageHasAnExternalBackButtonAndSubmitsOnIme() {
        var submitted = ""
        var closed = false
        compose.setContent { LedgerTheme {
            var input by remember { mutableStateOf("") }
            BillSearchHeader(input, { input = it }, { submitted = input }, { closed = true })
        } }
        val field = compose.onNode(hasSetTextAction())
        val back = compose.onNodeWithContentDescription("返回")
        assertTrue(back.fetchSemanticsNode().boundsInRoot.right < field.fetchSemanticsNode().boundsInRoot.left)
        field.performTextInput("午餐")
        field.performImeAction()
        compose.runOnIdle { assertEquals("午餐", submitted) }
        back.performClick()
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
