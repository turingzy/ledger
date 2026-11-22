package cn.turingzy.ledger.navigation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import kotlin.math.roundToInt

/** Shares the default layered transition across the app and settings navigation hosts. */
internal object PageTransitions {
    private const val DurationMillis = 300
    private const val BackgroundOffset = -0.30f
    private const val ForegroundAlpha = 0.8f

    val enter = slideInHorizontally(tween(DurationMillis, easing = FastOutSlowInEasing)) { it } +
        fadeIn(tween(DurationMillis, easing = FastOutSlowInEasing), initialAlpha = ForegroundAlpha)
    val exit = slideOutHorizontally(tween(DurationMillis, easing = FastOutSlowInEasing)) {
        (it * BackgroundOffset).roundToInt()
    }
    val popEnter = slideInHorizontally(tween(DurationMillis, easing = FastOutSlowInEasing)) {
        (it * BackgroundOffset).roundToInt()
    }
    val popExit = slideOutHorizontally(tween(DurationMillis, easing = FastOutSlowInEasing)) { it } +
        fadeOut(tween(DurationMillis, easing = FastOutSlowInEasing), targetAlpha = ForegroundAlpha)

    val taskEnter = slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { it }
    val taskPopExit = slideOutVertically(tween(250, easing = FastOutLinearInEasing)) { it }
}
