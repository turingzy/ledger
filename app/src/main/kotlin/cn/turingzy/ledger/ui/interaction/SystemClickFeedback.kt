package cn.turingzy.ledger.ui.interaction

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * 系统级点击反馈行为（触感与音效）的 [CompositionLocal]，便于在对话框或特殊容器中与宿主视图共享相同的反馈逻辑。
 */
internal val LocalSystemClickFeedback = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * 记住并返回一个触发系统标准点击反馈的函数。
 *
 * 优先使用 [LocalSystemClickFeedback]；若未提供则通过当前宿主 View 触发按键振动反馈（VIRTUAL_KEY）与点击音效（CLICK）。
 *
 * @return 触发点击反馈的无参回调函数
 */
@Composable
internal fun rememberSystemClickFeedback(): () -> Unit {
    LocalSystemClickFeedback.current?.let { return it }
    val view = LocalView.current
    return remember(view) {
        {
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }
}
