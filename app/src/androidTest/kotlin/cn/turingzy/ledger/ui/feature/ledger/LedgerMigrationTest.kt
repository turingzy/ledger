package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.ui.interaction.LocalUiInteractionClock
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LedgerMigrationTest {
    @get:Rule val compose = createComposeRule()
    private val state = LedgerUiState.Success(2026, 11, "2026.11", "11月结余", 0L, 0L, 0L, emptyList())

    @Test fun searchOnlySubmitsOnImeAndClosingResetsTheFilter() {
        val searches = mutableListOf<String>()
        compose.setContent { LedgerTheme { LedgerContent(state, onSearch = { searches += it }) } }
        compose.onNodeWithContentDescription("搜索").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(" 午餐 ")
        compose.runOnIdle { assertEquals(listOf(""), searches) }
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(listOf("", "午餐"), searches) }
        compose.onNodeWithContentDescription("关闭搜索").performClick()
        compose.runOnIdle { assertEquals(listOf("", "午餐", ""), searches) }
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("2026.11").assertIsDisplayed()
    }

    @Test fun dateEntryUsesThemeTextWhileKeepingSeventeenSp() {
        var foreground = Color.Unspecified
        compose.setContent { LedgerTheme(darkTheme = true) {
            foreground = LedgerStyle.colors.textPrimary
            LedgerContent(state)
        } }
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText("2026.11", useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(17.sp, results.single().layoutInput.style.fontSize)
        assertEquals(foreground, results.single().layoutInput.style.color)
    }

    @Test fun transactionClickUsesTheSharedGateAndPreservesTheSelectedId() {
        val case = LedgerPreviewProvider().values.first { (it.state as? LedgerUiState.Success)?.dayGroups?.isNotEmpty() == true }
        val populated = case.state as LedgerUiState.Success
        val clicks = mutableListOf<Long>()
        var now = 0L
        compose.setContent {
            CompositionLocalProvider(LocalUiInteractionClock provides { now }) {
                LedgerTheme { LedgerContent(populated, onTransactionClick = { clicks += it }) }
            }
        }
        compose.onNodeWithText("餐饮").performClick().performClick()
        compose.runOnIdle { assertEquals(listOf(1L), clicks); now = 400L }
        compose.onNodeWithText("餐饮").performClick()
        compose.runOnIdle { assertEquals(listOf(1L, 1L), clicks) }
    }
}
