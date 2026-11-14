package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

private class ChartSelectionBoundary(val dismiss: () -> Unit) {
    var bounds: Rect = Rect.Zero
}

private class ChartInteractions {
    val boundaries = mutableSetOf<ChartSelectionBoundary>()
    var origin = Offset.Zero
}

private val LocalChartInteractions = staticCompositionLocalOf<ChartInteractions?> { null }

@Composable
internal fun AnalyticsChartInteractionHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val interactions = remember { ChartInteractions() }
    CompositionLocalProvider(LocalChartInteractions provides interactions) {
        Box(modifier.onGloballyPositioned { interactions.origin = it.positionInRoot() }
            .pointerInput(interactions) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.firstOrNull { it.changedToDownIgnoreConsumed() }?.let { change ->
                            val position = change.position + interactions.origin
                            // Observe before child gestures consume the event, without interrupting scrolling or taps.
                            interactions.boundaries.filterNot { it.bounds.contains(position) }.forEach { it.dismiss() }
                        }
                    }
                }
            }) { content() }
    }
}

@Composable
internal fun Modifier.chartSelectionBoundary(onDismiss: () -> Unit): Modifier {
    val interactions = LocalChartInteractions.current
    val dismiss by rememberUpdatedState(onDismiss)
    val boundary = remember { ChartSelectionBoundary { dismiss() } }
    DisposableEffect(interactions, boundary) {
        interactions?.boundaries?.add(boundary)
        onDispose { interactions?.boundaries?.remove(boundary) }
    }
    return onGloballyPositioned { boundary.bounds = it.boundsInRoot() }
}
