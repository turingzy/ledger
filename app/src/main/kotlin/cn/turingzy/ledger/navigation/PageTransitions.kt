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

/**
 * 页面转场动画规范配置。
 *
 * 在应用主导航及设置等二级导航宿主之间共享一致的层叠视差与任务覆盖转场效果。
 */
internal object PageTransitions {
    /** 标准转场持续时长（毫秒）。 */
    private const val DurationMillis = 300
    /** 底层页面视差位移比例。 */
    private const val BackgroundOffset = -0.30f
    /** 前景页面淡入起始及淡出终止透明度。 */
    private const val ForegroundAlpha = 0.8f

    /**
     * 新页面推入入场动画：从右侧水平滑入并伴随透明度淡入。
     */
    val enter = slideInHorizontally(tween(DurationMillis, easing = FastOutSlowInEasing)) { it } +
        fadeIn(tween(DurationMillis, easing = FastOutSlowInEasing), initialAlpha = ForegroundAlpha)

    /**
     * 当前页面被新页面压栈退场动画：向左侧小幅视差位移。
     */
    val exit = slideOutHorizontally(tween(DurationMillis, easing = FastOutSlowInEasing)) {
        (it * BackgroundOffset).roundToInt()
    }

    /**
     * 出栈回退时底层页面重新进入前台的动画：从左侧小幅视差位移恢复居中。
     */
    val popEnter = slideInHorizontally(tween(DurationMillis, easing = FastOutSlowInEasing)) {
        (it * BackgroundOffset).roundToInt()
    }

    /**
     * 出栈回退时前台页面离开的动画：向右侧水平滑出并伴随透明度淡出。
     */
    val popExit = slideOutHorizontally(tween(DurationMillis, easing = FastOutSlowInEasing)) { it } +
        fadeOut(tween(DurationMillis, easing = FastOutSlowInEasing), targetAlpha = ForegroundAlpha)

    /**
     * 全屏临时任务页垂直向上滑入动画。
     */
    val taskEnter = slideInVertically(tween(300, easing = LinearOutSlowInEasing)) { it }

    /**
     * 全屏临时任务页关闭时垂直向下滑出退场动画。
     */
    val taskPopExit = slideOutVertically(tween(250, easing = FastOutLinearInEasing)) { it }
}
