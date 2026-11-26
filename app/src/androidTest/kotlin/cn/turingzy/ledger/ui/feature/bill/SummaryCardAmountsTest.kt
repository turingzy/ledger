package cn.turingzy.ledger.ui.feature.bill

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cn.turingzy.ledger.navigation.RetainedDestinationHost
import cn.turingzy.ledger.navigation.TopLevelDestination
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.turingzy.ledger.ui.component.card.summary.SummaryCard
import cn.turingzy.ledger.ui.feature.bill.component.SummaryHeroBanner
import cn.turingzy.ledger.ui.feature.wallet.WalletHeroCard
import cn.turingzy.ledger.ui.interaction.LocalUiInteractionClock
import cn.turingzy.ledger.ui.theme.LedgerTheme
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.util.toFormattedAmount
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
    fun eitherSwipeOpensTheCenteredImageButton() {
        compose.setContent {
            LedgerTheme {
                SummaryCard(modifier = Modifier.testTag("summaryCard")) {}
            }
        }
        val card = compose.onNodeWithTag("summaryCard")
        card.performTouchInput { swipeLeft(startX = width * 0.8f, endX = width * 0.2f) }
        val upload = compose.onNodeWithText("更换图片")
        upload.assertIsDisplayed()
        compose.onNodeWithText("透明度").assertDoesNotExist()
        val cardBounds = card.fetchSemanticsNode().boundsInRoot
        val uploadBounds = upload.fetchSemanticsNode().boundsInRoot
        assertEquals(cardBounds.center.x, uploadBounds.center.x, 1f)
        assertEquals(cardBounds.center.y, uploadBounds.center.y, 1f)
        assertTrue(uploadBounds.width > uploadBounds.height)
        val label = compose.onNodeWithText("更换图片", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val icon = compose.onNodeWithTag("baseButtonTrailingIcon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue(label.right <= icon.left)
        card.performTouchInput {
            swipe(Offset(width * 0.2f, height * 0.8f), Offset(width * 0.8f, height * 0.8f))
        }
        upload.assertDoesNotExist()
        card.performTouchInput { swipeRight(startX = width * 0.2f, endX = width * 0.8f) }
        upload.assertIsDisplayed()
        card.performTouchInput {
            swipe(Offset(width * 0.8f, height * 0.8f), Offset(width * 0.2f, height * 0.8f))
        }
        upload.assertDoesNotExist()
    }

    @Test
    fun photoPickerKeepsSettingsOpenUntilAnImageIsSelected() {
        var launchedRequestCode = -1
        var launches = 0
        var now = 0L
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                assertTrue(contract is ActivityResultContracts.PickVisualMedia)
                launchedRequestCode = requestCode
                launches++
            }
        }
        val registryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        val lifecycleOwner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }
        lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
        val image = Uri.parse("content://ledger-test/background")
        val selected = mutableListOf<Uri>()
        compose.setContent {
            CompositionLocalProvider(
                LocalActivityResultRegistryOwner provides registryOwner,
                LocalLifecycleOwner provides lifecycleOwner,
                LocalUiInteractionClock provides { now },
            ) {
                LedgerTheme {
                    SummaryCard(Modifier.testTag("summaryCard"), onBackgroundSelected = { selected += it }) {
                        Text("汇总信息")
                    }
                }
            }
        }
        compose.onNodeWithTag("summaryCard").performTouchInput { swipeLeft() }
        val upload = compose.onNodeWithText("更换图片")
        upload.performClick()
        compose.runOnIdle {
            assertEquals(1, launches)
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
        }
        upload.assertIsDisplayed()
        compose.onNodeWithText("汇总信息").assertDoesNotExist()
        compose.runOnIdle {
            registry.dispatchResult<Uri?>(launchedRequestCode, null)
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
            assertTrue(selected.isEmpty())
            now = 1_000L
        }
        upload.assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(2, launches)
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
            registry.dispatchResult(launchedRequestCode, image)
            lifecycleOwner.lifecycle.currentState = Lifecycle.State.RESUMED
            assertEquals(listOf(image), selected)
        }
        compose.onNodeWithText("汇总信息").assertIsDisplayed()
        upload.assertDoesNotExist()
    }

    @Test
    fun longAmountsFitOnOneLineWithTabularDigits() {
        val balance = Long.MIN_VALUE
        val expense = Long.MAX_VALUE
        val income = expense - 1
        compose.setContent {
            LedgerTheme {
                SummaryHeroBanner("结余", balance, expense, income, modifier = Modifier.width(224.dp))
            }
        }
        listOf(balance, expense, income).forEach { amount ->
            val text = amount.toFormattedAmount(showSymbol = true, useGrouping = true)
            val results = mutableListOf<TextLayoutResult>()
            compose.onNodeWithText(text)
                .assertIsDisplayed()
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
            val layout = results.single()
            assertEquals("tnum", layout.layoutInput.style.fontFeatureSettings)
            assertEquals(1, layout.lineCount)
            assertEquals(text.length, layout.getLineEnd(0, visibleEnd = true))
            assertTrue(!layout.hasVisualOverflow)
            val maximum = if (amount == balance) LedgerStyle.typography.heroAmount else LedgerStyle.typography.body
            assertTrue(layout.layoutInput.style.fontSize < maximum.fontSize)
        }
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
        compose.onNodeWithText("更换图片").assertIsDisplayed()
        compose.runOnIdle { active = TopLevelDestination.WALLET }
        compose.runOnIdle { active = TopLevelDestination.BILL }
        compose.onNodeWithText("更换图片").assertDoesNotExist()
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
        compose.onNodeWithText("更换图片").assertIsDisplayed()
        card.performTouchInput {
            swipe(Offset(width * 0.2f, height * 0.8f), Offset(width * 0.8f, height * 0.8f))
        }
        compose.onAllNodesWithText("****").assertCountEquals(3)
        compose.onNodeWithContentDescription("显示卡片金额").performClick()
        compose.onAllNodesWithText("****").assertCountEquals(0)
        amounts.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
    }
}
