package cn.turingzy.ledger.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner

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

    // Keep tab compositions alive without measuring or drawing the hidden pages.
    Layout(
        modifier = modifier,
        content = {
            destinations.forEach { destination ->
                key(destination) {
                    val owner = rememberDestinationLifecycleOwner(destination == currentDestination)
                    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                        Box(Modifier.fillMaxSize()) { content(destination) }
                    }
                }
            }
        },
    ) { measurables, constraints ->
        val selected = measurables[destinations.indexOf(currentDestination)].measure(constraints)
        layout(selected.width, selected.height) { selected.placeRelative(0, 0) }
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
