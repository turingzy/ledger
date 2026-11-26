package cn.turingzy.ledger.ui.component.card.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Covers the visible animation and the cases where the previous amount must stay out of view. */
class SummaryAmountTickerTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun changedDigitRollsAfterSheetClosesAndHiddenChangesAppearSettled() {
        var amount by mutableLongStateOf(12_345L)
        var obscured by mutableStateOf(false)
        var hidden by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalSummaryAmountsObscured provides obscured) {
                Box(Modifier.background(Color.White)) {
                    SummaryAmountTicker(
                        amount = amount,
                        currencySymbol = "¥",
                        hidden = hidden,
                        animate = true,
                        style = TextStyle(color = Color.Black, fontSize = 26.sp),
                    )
                }
            }
        }

        compose.onNodeWithText("¥123.45").assertIsDisplayed()
        compose.runOnIdle { obscured = true }
        compose.runOnIdle { amount = 12_346L }
        compose.onNodeWithText("¥123.45").assertIsDisplayed()
        compose.onNodeWithText("¥123.46").assertDoesNotExist()

        val updated = compose.onNodeWithText("¥123.46")
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnIdle { obscured = false }
            compose.mainClock.advanceTimeBy(128)
            val rolling = updated.captureToImage().toPixelMap()
            compose.mainClock.advanceTimeBy(480)
            val settled = updated.captureToImage().toPixelMap()
            assertEquals(rolling.width, settled.width)
            assertEquals(rolling.height, settled.height)
            assertTrue((0 until settled.height).any { y ->
                (0 until settled.width).any { x -> rolling[x, y] != settled[x, y] }
            })

            compose.runOnIdle { hidden = true }
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithText("****").assertIsDisplayed()
            compose.runOnIdle { amount = 12_347L }
            compose.mainClock.advanceTimeByFrame()
            compose.onNodeWithText("¥123.47").assertDoesNotExist()

            compose.runOnIdle { hidden = false }
            compose.mainClock.advanceTimeBy(64)
            val revealed = compose.onNodeWithText("¥123.47")
            val first = revealed.captureToImage().toPixelMap()
            compose.mainClock.advanceTimeBy(480)
            val settledAfterReveal = revealed.captureToImage().toPixelMap()
            assertEquals(first.width, settledAfterReveal.width)
            assertEquals(first.height, settledAfterReveal.height)
            assertTrue((0 until first.height).all { y ->
                (0 until first.width).all { x -> first[x, y] == settledAfterReveal[x, y] }
            })
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }
}
