package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.turingzy.ledger.ui.feature.wallet.WalletHeroCard
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Rule
import org.junit.Test

class SummaryCardAmountsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun ledgerHidesAllAmountsAndKeepsThemHiddenAfterCardSettings() {
        assertAmountsToggle("结余") {
            SummaryHeroBanner("结余", balance = 12345L, expense = 456L, income = 12801L)
        }
    }

    @Test
    fun walletHidesAllAmountsAndKeepsThemHiddenAfterCardSettings() {
        assertAmountsToggle("总资产") {
            WalletHeroCard(totalAsset = 12345L, totalDebt = 456L)
        }
    }

    private fun assertAmountsToggle(title: String, content: @Composable () -> Unit) {
        compose.setContent { LedgerTheme { content() } }
        val amounts = listOf("¥123.45", "¥4.56", "¥128.01")
        amounts.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithContentDescription("隐藏卡片金额").performClick()
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onAllNodesWithText("****").assertCountEquals(3)
        amounts.forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        compose.onNodeWithContentDescription("更多卡片设置").performClick()
        compose.onNodeWithContentDescription("关闭卡片设置").performClick()
        compose.onAllNodesWithText("****").assertCountEquals(3)
        compose.onNodeWithContentDescription("显示卡片金额").performClick()
        compose.onAllNodesWithText("****").assertCountEquals(0)
        amounts.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }
}
