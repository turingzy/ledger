package cn.turingzy.ledger.ui.component.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.theme.LedgerTheme
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

/** Verifies that pressing the button only changes pixels inside the scalloped face. */
class LedgerFloatingActionButtonTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun pressHighlightsFaceWithoutTintingTheNotch() {
        compose.setContent {
            LedgerTheme {
                Box(Modifier.background(Color.White).padding(12.dp)) {
                    LedgerFloatingActionButton(
                        visible = true,
                        onClick = {},
                        modifier = Modifier.testTag("recordButton"),
                    )
                }
            }
        }
        val button = compose.onNodeWithTag("recordButton")
        val before = button.captureToImage().toPixelMap()
        val faceX = before.width * 3 / 4
        val faceY = before.height / 2
        // The first notch is 20 degrees clockwise from the top edge of the button.
        val notchX = (before.width * 0.663f).roundToInt()
        val notchY = (before.height * 0.05f).roundToInt()

        compose.mainClock.autoAdvance = false
        try {
            button.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(160)
            val pressed = button.captureToImage().toPixelMap()
            assertNotEquals(before[faceX, faceY], pressed[faceX, faceY])
            assertEquals(before[notchX, notchY], pressed[notchX, notchY])
            button.performTouchInput { cancel() }
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }
}
