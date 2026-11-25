package cn.turingzy.ledger.ui.feature.bill.search

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import cn.turingzy.ledger.MainActivity
import org.junit.Rule
import org.junit.Test

class BillSearchNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun ledgerContentRemainsAfterClosingSearchAndSwitchingTabs() {
        val ledgerPeriod = hasContentDescription("选择账本日期") or hasContentDescription("Select ledger period")
        val openSearch = hasContentDescription("搜索") or hasContentDescription("Search")
        val closeSearch = hasContentDescription("关闭搜索") or hasContentDescription("Close search")
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(ledgerPeriod).fetchSemanticsNodes().isNotEmpty()
        }

        repeat(2) {
            compose.onNode(openSearch).performClick()
            compose.onNode(closeSearch).assertIsDisplayed().performClick()
            compose.onNode(ledgerPeriod).assertIsDisplayed()
        }

        val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        compose.onNode(tab and (hasText("设置") or hasText("Settings"))).performClick()
        compose.onNode(tab and (hasText("账本") or hasText("Ledger"))).performClick()
        compose.onNode(ledgerPeriod).assertIsDisplayed()
    }

    @Test
    fun noResultsAppearsOnlyAfterAnUnmatchedSearch() {
        val openSearch = hasContentDescription("搜索") or hasContentDescription("Search")
        val noResults = hasText("未找到匹配的交易") or hasText("No matching transactions")
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(openSearch).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(openSearch).performClick()
        compose.onNode(noResults).assertDoesNotExist()

        val field = compose.onNode(hasSetTextAction())
        field.performTextInput("no_result_8c91607a4e")
        field.performImeAction()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(noResults).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(noResults).assertIsDisplayed()
    }
}
