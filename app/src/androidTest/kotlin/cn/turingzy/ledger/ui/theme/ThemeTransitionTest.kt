package cn.turingzy.ledger.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Verifies the top-right reveal and preserves the composition across a theme switch. */
class ThemeTransitionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun revealsFromTopRightWithoutRemountingContent() {
        val dark = mutableStateOf(false)
        var mounted = 0
        var disposed = 0
        var displayedDark = false
        compose.setContent {
            Box(Modifier.size(160.dp)) {
                ThemeWipe(dark.value) { topDark, _ ->
                    Box(Modifier.fillMaxSize().testTag("themeFrame").background(if (topDark) Color.Black else Color.White))
                    SideEffect { displayedDark = topDark }
                    DisposableEffect(Unit) {
                        mounted++
                        onDispose { disposed++ }
                    }
                }
            }
        }
        val frame = compose.onNodeWithTag("themeFrame")
        compose.mainClock.autoAdvance = false
        try {
            compose.runOnIdle { dark.value = true }
            repeat(20) {
                if (!compose.runOnIdle { displayedDark }) {
                    compose.mainClock.advanceTimeByFrame()
                    frame.captureToImage()
                }
            }
            assertTrue(compose.runOnIdle { displayedDark })
            compose.mainClock.advanceTimeBy(240)
            val midpoint = frame.captureToImage().toPixelMap()
            assertEquals(Color.Black, midpoint[midpoint.width * 9 / 10, midpoint.height / 10])
            assertEquals(Color.White, midpoint[midpoint.width / 10, midpoint.height / 10])
            assertEquals(Color.White, midpoint[midpoint.width / 10, midpoint.height * 9 / 10])
            compose.mainClock.advanceTimeBy(400)
            val settled = frame.captureToImage().toPixelMap()
            assertEquals(Color.Black, settled[settled.width / 2, settled.height * 9 / 10])
            compose.runOnIdle {
                assertEquals(1, mounted)
                assertEquals(0, disposed)
            }
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }
}
