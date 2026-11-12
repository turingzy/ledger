package cn.turingzy.ledger.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun <T : Any> SettingsSubpageTransition(
    targetState: T,
    depth: (T) -> Int,
    contentKey: (T) -> Any = { it },
    content: @Composable (T) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val forward = depth(targetState) > depth(initialState)
            slideInHorizontally(tween(240)) { if (forward) it else -it / 4 } togetherWith
                slideOutHorizontally(tween(240)) { if (forward) -it / 4 else it }
        },
        contentKey = contentKey,
        label = "SettingsSubpage",
    ) { content(it) }
}
