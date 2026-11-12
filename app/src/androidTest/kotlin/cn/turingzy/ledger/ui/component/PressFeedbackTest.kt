package cn.turingzy.ledger.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class PressFeedbackTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun pressChangesEntireBackgroundAtOnceAndCancelRestoresIt() {
        compose.setContent {
            LedgerTheme {
                Box(Modifier.size(80.dp).testTag("pressTarget").background(MaterialTheme.colorScheme.surface).clickable {})
            }
        }
        val target = compose.onNodeWithTag("pressTarget")
        val before = target.captureToImage().toPixelMap()
        target.performTouchInput { down(center) }
        compose.waitForIdle()
        val pressed = target.captureToImage().toPixelMap()
        assertNotEquals(before[10, 10], pressed[10, 10])
        assertEquals(pressed[10, 10], pressed[pressed.width / 2, pressed.height / 2])
        target.performTouchInput { cancel() }
        compose.waitForIdle()
        val released = target.captureToImage().toPixelMap()
        assertEquals(before[10, 10], released[10, 10])
    }
    @Test
    fun quickTopBarTapShowsACircularBackgroundInsideScrollableContent() {
        var clicks = 0
        compose.setContent {
            LedgerTheme {
                Column(Modifier.background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState())) {
                    TopBarIconButton(MaterialSymbols.AddCard, "添加账户", { clicks++ }, Modifier.testTag("topBar"))
                }
            }
        }
        val target = compose.onNodeWithTag("topBar")
        val before = target.captureToImage().toPixelMap()
        compose.mainClock.autoAdvance = false
        target.performTouchInput { down(center); up() }
        compose.mainClock.advanceTimeBy(80)
        val feedback = target.captureToImage().toPixelMap()
        assertNotEquals(before[3, before.height / 2], feedback[3, feedback.height / 2])
        assertEquals(before[1, 1], feedback[1, 1])
        compose.runOnIdle { assertEquals(1, clicks) }
        compose.mainClock.advanceTimeBy(400)
        val released = target.captureToImage().toPixelMap()
        assertEquals(before[3, before.height / 2], released[3, released.height / 2])
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun segmentedControlDoesNotPaintAnotherSelectionWhilePressed() {
        compose.setContent {
            LedgerTheme {
                LedgerSegmentedControl(listOf("支出", "收入"), 0, {}, Modifier.testTag("segments"))
            }
        }
        val target = compose.onNodeWithTag("segments")
        val before = target.captureToImage().toPixelMap()
        val income = compose.onNodeWithText("收入")
        income.performTouchInput { down(center) }
        compose.waitForIdle()
        val pressed = target.captureToImage().toPixelMap()
        assertEquals(before[before.width * 3 / 4, 3], pressed[pressed.width * 3 / 4, 3])
        income.performTouchInput { cancel() }
    }

    @Test
    fun dismissIconCanDisablePressFeedback() {
        compose.setContent {
            LedgerTheme {
                Box(Modifier.background(MaterialTheme.colorScheme.surface)) {
                    LedgerIconButton({}, Modifier.testTag("dismiss"), showPressFeedback = false) {}
                }
            }
        }
        val target = compose.onNodeWithTag("dismiss")
        val before = target.captureToImage().toPixelMap()
        target.performTouchInput { down(center) }
        compose.waitForIdle()
        val pressed = target.captureToImage().toPixelMap()
        assertEquals(before[before.width / 2, before.height / 2], pressed[pressed.width / 2, pressed.height / 2])
        target.performTouchInput { cancel() }
    }

}
