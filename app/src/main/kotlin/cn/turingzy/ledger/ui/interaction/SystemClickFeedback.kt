package cn.turingzy.ledger.ui.interaction

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/** Lets controls in dialogs use the same host view as the app's navigation bar. */
internal val LocalSystemClickFeedback = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Plays Android's click sound and key haptic feedback through the host view. */
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
