package cn.turingzy.ledger.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.navigation.TopLevelDestination
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NavigationAndScrollHeaderTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun navigationLabelsRemainVisibleAndSelectionMovesAfterTapping() {
        var selected by mutableStateOf(TopLevelDestination.LEDGER)
        var navigationCount = 0
        compose.setContent {
            LedgerTheme {
                LedgerBottomBar(TopLevelDestination.entries, selected, {
                    selected = it
                    navigationCount++
                })
            }
        }
        listOf("账本", "分析", "钱包", "设置").forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithText("账本").assertIsSelected().performClick()
        compose.runOnIdle { assertEquals(0, navigationCount) }
        compose.onNodeWithText("钱包").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(1, navigationCount) }
    }

    @Test
    fun compactHeaderArrowReturnsTheListToTheBeginning() {
        lateinit var listState: LazyListState
        compose.setContent {
            LedgerTheme {
                listState = rememberLazyListState()
                ScrollTitleContainer(
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
