package cn.turingzy.ledger.ui.interaction

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import cn.turingzy.ledger.ui.components.base.BaseButton
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class UiInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun repeatedClicksAreSuppressedWithoutStaleCallbacksOrDuplicateHaptics() {
        var now = 0L
        val clock = { now }
        val calls = mutableListOf<String>()
        val haptics = mutableListOf<HapticFeedbackType>()
        val feedback = object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { haptics.add(hapticFeedbackType) }
        }
        var label by mutableStateOf("first")
        compose.setContent {
            CompositionLocalProvider(LocalUiInteractionClock provides clock, LocalHapticFeedback provides feedback) {
                LedgerTheme {
                    BaseButton("选择", { calls.add(label) }, interaction = UiInteractionKind.Selection)
                }
            }
        }
        compose.onNodeWithText("选择").performClick().performClick()
        compose.runOnIdle {
            assertEquals(listOf("first"), calls)
            assertEquals(listOf(HapticFeedbackType.TextHandleMove), haptics)
            label = "latest"
        }
        compose.onNodeWithText("选择").performClick()
        compose.runOnIdle {
            assertEquals(1, calls.size)
            now = 350L
        }
        compose.onNodeWithText("选择").performClick()
        compose.runOnIdle {
            assertEquals(listOf("first", "latest"), calls)
            assertEquals(2, haptics.size)
        }
    }

    @Test
    fun aQuickTapShowsFeedbackAndThenRestoresItsBackground() {
        compose.setContent { LedgerTheme { BaseButton("按钮", {}, Modifier.testTag("quick")) } }
        val target = compose.onNodeWithTag("quick")
        val before = target.captureToImage().toPixelMap()
        compose.mainClock.autoAdvance = false
        try {
            target.performTouchInput { down(center); up() }
            compose.mainClock.advanceTimeBy(80)
            val pressed = target.captureToImage().toPixelMap()
            assertNotEquals(before[before.width / 2, 3], pressed[pressed.width / 2, 3])
            compose.mainClock.advanceTimeBy(400)
            val released = target.captureToImage().toPixelMap()
            assertEquals(before[before.width / 2, 3], released[released.width / 2, 3])
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }
}
