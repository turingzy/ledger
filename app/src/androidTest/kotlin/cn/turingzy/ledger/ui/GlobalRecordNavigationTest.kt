package cn.turingzy.ledger.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import cn.turingzy.ledger.MainActivity
import org.junit.Rule
import org.junit.Test

/** 验证四个一级页面均可就地记账，返回键只关闭记账抽屉。 */
class GlobalRecordNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun recordSheetOpensAndClosesOnEveryTab() {
        val record = hasContentDescription("记一笔") or hasContentDescription("Record transaction")
        val tab = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        val tabs = listOf("账本" to "Ledger", "分析" to "Analytics", "钱包" to "Wallet", "设置" to "Settings")
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodes(record).fetchSemanticsNodes().isNotEmpty()
        }

        tabs.forEach { (chinese, english) ->
            val destination = compose.onNode(tab and (hasText(chinese) or hasText(english)))
            destination.performClick().assertIsSelected()
            compose.onNode(record).assertIsDisplayed().performClick()
            compose.onNodeWithTag("recordDrawer").assertIsDisplayed()

            pressBack()

            compose.onNodeWithTag("recordDrawer").assertDoesNotExist()
            destination.assertIsSelected()
        }
    }
}
