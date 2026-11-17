package cn.turingzy.ledger.ui.components.base

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.theme.LedgerTheme
import cn.turingzy.ledger.ui.theme.LedgerStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BasePageScaffoldTest {
    @get:Rule val compose = createComposeRule()

    @Test fun safeInsetsAreAppliedOnce() = verifyInsets(parentConsumesInsets = false)
    @Test fun aParentConsumingInsetsDoesNotDoubleThePadding() = verifyInsets(parentConsumesInsets = true)

    @Test
    fun scrollingContentFeedsTheFixedFrostedTopBar() {
        compose.setContent {
            LedgerTheme {
                val listState = rememberLazyListState()
                Box(Modifier.size(320.dp)) {
                    BasePageScaffold("", Modifier.testTag("page"), scrolled = listState.canScrollBackward,
                        windowInsets = WindowInsets(0, 0, 0, 0)) { padding ->
                        LazyColumn(Modifier.fillMaxSize().testTag("scroll"), state = listState, contentPadding = padding) {
                            items(8, key = { it }) { index ->
                                Box(Modifier.fillMaxWidth().height(LedgerStyle.dimensions.touchTarget * 2)
                                    .background(LedgerStyle.colors.info).testTag("item-$index"))
                            }
                        }
                    }
                }
            }
        }
        val bar = compose.onNodeWithTag("basePageTopBar").fetchSemanticsNode().boundsInRoot
        val before = compose.onNodeWithTag("page").captureToImage().toPixelMap()
        compose.onNodeWithTag("scroll").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, bar.height / 2) }
        compose.waitForIdle()
        val after = compose.onNodeWithTag("page").captureToImage().toPixelMap()
        val sampleY = (bar.bottom * 0.85f).toInt()
        assertTrue(compose.onNodeWithTag("item-0").fetchSemanticsNode().boundsInRoot.top < bar.bottom)
        assertEquals(bar, compose.onNodeWithTag("basePageTopBar").fetchSemanticsNode().boundsInRoot)
        assertNotEquals(before[before.width / 2, sampleY], after[after.width / 2, sampleY])
    }

    private fun verifyInsets(parentConsumesInsets: Boolean) {
        compose.setContent {
            LedgerTheme {
                val insets = WindowInsets(left = 12.dp, top = 24.dp, right = 16.dp, bottom = 32.dp)
                Box(Modifier.size(width = 320.dp, height = 480.dp).then(
                    if (parentConsumesInsets) Modifier.windowInsetsPadding(insets) else Modifier,
                )) {
                    BasePageScaffold("标题", windowInsets = insets) { padding ->
                        Box(Modifier.fillMaxSize().padding(padding).testTag("content"))
                    }
                }
            }
        }
        compose.onNodeWithTag("content")
            .assertLeftPositionInRootIsEqualTo(12.dp)
            .assertTopPositionInRootIsEqualTo(80.dp)
            .assertWidthIsEqualTo(292.dp)
    }
}
