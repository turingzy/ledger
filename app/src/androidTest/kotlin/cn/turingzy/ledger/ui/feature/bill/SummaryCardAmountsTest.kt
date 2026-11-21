package cn.turingzy.ledger.ui.feature.bill

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.turingzy.ledger.navigation.RetainedDestinationHost
import cn.turingzy.ledger.navigation.TopLevelDestination
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import cn.turingzy.ledger.ui.component.card.summary.SummaryCard
import cn.turingzy.ledger.ui.feature.bill.component.SummaryHeroBanner
import cn.turingzy.ledger.ui.feature.wallet.WalletHeroCard
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            WalletHeroCard(totalAsset = 12345L, totalExpense = 456L, totalIncome = 12801L)
        }
    }

    @Test
    fun eitherSwipeOpensSettingsAndSliderKeepsSettingsOpen() {
        val saved = mutableListOf<Int>()
        compose.setContent {
            LedgerTheme {
                SummaryCard(
                    modifier = Modifier.testTag("summaryCard"),
                    onTransparencyChange = { saved += it },
                ) {}
            }
        }
        val card = compose.onNodeWithTag("summaryCard")
        card.performTouchInput { swipeLeft(startX = width * 0.8f, endX = width * 0.2f) }
        val slider = compose.onNodeWithContentDescription("透明度")
        slider.assertIsDisplayed()
        val sliderBounds = slider.fetchSemanticsNode().boundsInRoot
        val uploadBounds = compose.onNodeWithContentDescription("更换卡片背景").fetchSemanticsNode().boundsInRoot
        assertEquals(sliderBounds.center.y, uploadBounds.center.y, 1f)
        assertTrue(sliderBounds.right < uploadBounds.center.x)
        slider.performTouchInput { swipeRight() }
        slider.assertIsDisplayed()
        compose.runOnIdle { assertTrue(saved.single() > 0) }

        card.performTouchInput {
            swipe(Offset(width * 0.2f, height * 0.8f), Offset(width * 0.8f, height * 0.8f))
        }
        slider.assertDoesNotExist()
        card.performTouchInput { swipeRight(startX = width * 0.2f, endX = width * 0.8f) }
        slider.assertIsDisplayed()
        card.performTouchInput {
            swipe(Offset(width * 0.8f, height * 0.8f), Offset(width * 0.2f, height * 0.8f))
        }
        slider.assertDoesNotExist()
    }

    @Test
    fun switchingTabsResetsTheSettingsFaceOnARetainedCard() {
        var active by mutableStateOf(TopLevelDestination.BILL)
        compose.setContent { LedgerTheme {
            RetainedDestinationHost(active) { destination ->
                if (destination == TopLevelDestination.BILL) SummaryCard(Modifier.testTag("summaryCard")) {}
            }
        } }
        compose.onNodeWithTag("summaryCard").performTouchInput {
            swipeLeft(startX = width * 0.8f, endX = width * 0.2f)
        }
        compose.onNodeWithContentDescription("透明度").assertIsDisplayed()
        compose.runOnIdle { active = TopLevelDestination.WALLET }
        compose.runOnIdle { active = TopLevelDestination.BILL }
        compose.onNodeWithContentDescription("透明度").assertDoesNotExist()
    }

    private fun assertAmountsToggle(title: String, content: @Composable () -> Unit) {
        compose.setContent {
            LedgerTheme { Box(Modifier.testTag("summaryCard")) { content() } }
        }
        val amounts = listOf("¥123.45", "¥4.56", "¥128.01")
        amounts.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithContentDescription("隐藏卡片金额").performClick()
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onAllNodesWithText("****").assertCountEquals(3)
        amounts.forEach { compose.onNodeWithText(it).assertDoesNotExist() }
        val card = compose.onNodeWithTag("summaryCard")
        card.performTouchInput { swipeLeft(startX = width * 0.8f, endX = width * 0.2f) }
        compose.onNodeWithContentDescription("透明度").assertIsDisplayed()
        card.performTouchInput {
            swipe(Offset(width * 0.2f, height * 0.8f), Offset(width * 0.8f, height * 0.8f))
        }
        compose.onAllNodesWithText("****").assertCountEquals(3)
        compose.onNodeWithContentDescription("显示卡片金额").performClick()
        compose.onAllNodesWithText("****").assertCountEquals(0)
        amounts.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }
}
