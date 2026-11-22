package cn.turingzy.ledger.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.turingzy.ledger.ui.theme.LedgerStyle

private const val TabMinScale = 0.96f
private val TabTransitionSpec = tween<Float>(150, easing = FastOutSlowInEasing)

/** Animates changes between retained tabs while keeping their navigation and scroll state. */
@Composable
internal fun RetainedDestinationHost(
    currentDestination: TopLevelDestination,
    modifier: Modifier = Modifier,
    content: @Composable (TopLevelDestination) -> Unit,
) {
    val visited = remember { mutableStateListOf(currentDestination) }
    val destinations = (visited + currentDestination).distinct()
    SideEffect {
        if (currentDestination !in visited) visited.add(currentDestination)
    }

    // Tab selection happens outside the per-tab NavHosts so route pushes can keep their own motion.
    val transition = updateTransition(currentDestination, label = "Main tab")
    val progress = destinations.map { destination ->
        key(destination) {
            transition.animateFloat(
                transitionSpec = { TabTransitionSpec },
                label = destination.name,
            ) { if (it == destination) 1f else 0f }
        }
    }
    val visible = progress.map { progressState ->
        remember(progressState) { derivedStateOf { progressState.value > 0f } }
    }
    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            destinations.forEachIndexed { index, destination ->
                key(destination) {
                    val active = destination == currentDestination
                    val owner = rememberDestinationLifecycleOwner(active)
                    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                        val inactiveSemantics = if (active) Modifier else Modifier.semantics { hideFromAccessibility() }
                        Box(
                            Modifier.fillMaxSize()
                                .graphicsLayer {
                                    alpha = progress[index].value
                                    val scale = TabMinScale + (1f - TabMinScale) * alpha
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .then(inactiveSemantics)
                                .background(LedgerStyle.colors.background),
                        ) { content(destination) }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val selectedIndex = destinations.indexOf(currentDestination)
        val selected = measurables[selectedIndex].measure(constraints)
        val outgoing = measurables.mapIndexedNotNull { index, measurable ->
            measurable.takeIf { index != selectedIndex && visible[index].value }?.measure(constraints)
        }
        layout(selected.width, selected.height) {
            outgoing.forEach { it.placeRelative(0, 0) }
            selected.placeRelative(0, 0, zIndex = 1f)
        }
    }
}

@Composable
private fun rememberDestinationLifecycleOwner(active: Boolean): LifecycleOwner {
    val parent = LocalLifecycleOwner.current
    val owner = remember(parent) { DestinationLifecycleOwner(parent, active) }
    DisposableEffect(parent, owner) {
        parent.lifecycle.addObserver(owner)
        onDispose {
            parent.lifecycle.removeObserver(owner)
            owner.destroy()
        }
    }
    SideEffect { owner.setActive(active) }
    return owner
}

private class DestinationLifecycleOwner(
    private val parent: LifecycleOwner,
    active: Boolean,
) : LifecycleOwner, LifecycleEventObserver {
    override val lifecycle = LifecycleRegistry(this)
    private var maxState = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED

    init {
        syncState()
    }

    fun setActive(active: Boolean) {
        maxState = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
        syncState()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) = syncState()

    fun destroy() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
    }

    private fun syncState() {
        // Cached tabs keep their data current; backgrounding the app still stops collection.
        lifecycle.currentState = minOf(parent.lifecycle.currentState, maxState)
    }
}
