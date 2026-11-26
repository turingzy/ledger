package cn.turingzy.ledger.ui.component.navigation

import cn.turingzy.ledger.ui.component.base.ScrollTitleContainerBase
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.navigation.TopLevelDestination
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class NavigationAndScrollHeaderTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun hiddenBottomBarLetsTouchesReachTheBillActionUnderneath() {
        var actionCount = 0
        compose.setContent {
            LedgerTheme {
                Box(Modifier.fillMaxSize()) {
                    TextButton(onClick = { actionCount++ }, modifier = Modifier.align(Alignment.BottomCenter)
                        .navigationBarsPadding().padding(bottom = BottomBarVerticalPadding)
                        .height(BottomBarHeight).testTag("billAction")) { Text("编辑") }
                    LedgerBottomBar(TopLevelDestination.entries, TopLevelDestination.BILL, {},
                        modifier = Modifier.align(Alignment.BottomCenter), visible = false)
                }
            }
        }
        compose.onNodeWithTag("billAction").performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, actionCount) }
    }

    @Test
    fun navigationLabelsRemainVisibleAndSelectionMovesAfterTapping() {
        var selected by mutableStateOf(TopLevelDestination.BILL)
        var navigationCount = 0
        compose.setContent {
            LedgerTheme {
                LedgerBottomBar(TopLevelDestination.entries, selected, {
                    selected = it
                    navigationCount++
                })
            }
        }
        val labels = listOf("账本", "钱包", "统计", "设置")
        labels.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        val positions = labels.map { compose.onNodeWithText(it, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.center.x }
        assertEquals(positions.sorted(), positions)
        val walletLabel = compose.onNodeWithText("钱包", useUnmergedTree = true)
        val walletLabelBounds = walletLabel.fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("账本").assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(0, navigationCount) }
        compose.onNodeWithText("钱包").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(1, navigationCount) }
        assertEquals(walletLabelBounds, walletLabel.fetchSemanticsNode().boundsInRoot)
    }

    @Test
    fun quickTabTapShowsFeedbackAndCancelClearsItWithoutNavigating() {
        var navigationCount = 0
        compose.setContent {
            LedgerTheme {
                LedgerBottomBar(TopLevelDestination.entries, TopLevelDestination.BILL, { navigationCount++ })
            }
        }
        val tab = compose.onNodeWithText("钱包")
        val baselineImage = tab.captureToImage().toPixelMap()
        val x = baselineImage.width / 4
        val y = baselineImage.height / 2
        val baseline = baselineImage[x, y]
        compose.mainClock.autoAdvance = false
        try {
            tab.performTouchInput { down(center); up() }
            compose.mainClock.advanceTimeBy(80)
            assertNotEquals(baseline, tab.captureToImage().toPixelMap()[x, y])
            compose.mainClock.advanceTimeBy(400)
            assertEquals(baseline, tab.captureToImage().toPixelMap()[x, y])
            assertEquals(1, navigationCount)

            tab.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(160)
            assertNotEquals(baseline, tab.captureToImage().toPixelMap()[x, y])
            tab.performTouchInput { cancel() }
            compose.mainClock.advanceTimeBy(160)
            assertEquals(baseline, tab.captureToImage().toPixelMap()[x, y])
            assertEquals(1, navigationCount)
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test
    fun compactHeaderArrowReturnsTheListToTheBeginning() {
        lateinit var listState: LazyListState
        compose.setContent {
            LedgerTheme {
                listState = rememberLazyListState()
                ScrollTitleContainerBase(
                    title = "账本",
                    scrollOffset = {
                        if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset
                        else Int.MAX_VALUE
                    },
                    onScrollToTop = { listState.animateScrollToItem(0) },
                ) { scrollModifier ->
                    LazyColumn(scrollModifier.fillMaxSize().testTag("scrollContent"), state = listState) {
                        items(60, key = { it }) { Text("第${it}行", Modifier.height(48.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("scrollContent").performScrollToIndex(30)
        compose.onNodeWithText("账本").assertIsDisplayed()
        compose.onNodeWithContentDescription("回到顶部").performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(0, listState.firstVisibleItemIndex)
            assertEquals(0, listState.firstVisibleItemScrollOffset)
        }
        compose.onNodeWithContentDescription("回到顶部").assertDoesNotExist()
    }
}
