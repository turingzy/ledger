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

/** 非激活标签页在过渡时的最小缩放比例。 */
private const val TabMinScale = 0.96f
/** 标签页切换动画规格配置（150ms 缓动）。 */
private val TabTransitionSpec = tween<Float>(150, easing = FastOutSlowInEasing)

/**
 * 顶级目的地持久保留宿主容器。
 *
 * 保持已访问的顶级标签页（如账单、统计、钱包、设置）在切换时维持各自的组合树、导航回退栈与滚动状态，
 * 同时配合缩放与透明度实现平滑的标签页切换过渡动效，并对非激活页面进行无障碍隐藏。
 *
 * @param currentDestination 当前激活的顶级目的地。
 * @param modifier 容器修饰符。
 * @param content 针对各顶级目的地渲染内容的 Composable 组件。
 */
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

/**
 * 记住并管理指定标签页目的地专属的生命周期拥有者 [LifecycleOwner]。
 *
 * @param active 该目的地当前是否处于前台激活状态。
 */
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

/**
 * 标签页专属的 [LifecycleOwner] 实现。
 *
 * 根据当前标签页激活状态控制最高生命周期：
 * - 激活时最高为 [Lifecycle.State.RESUMED]；
 * - 切入后台时降级为 [Lifecycle.State.STARTED]，此时可维持后台数据流收集；
 * - 应用整体退至后台时跟随父生命周期挂起。
 */
private class DestinationLifecycleOwner(
    private val parent: LifecycleOwner,
    active: Boolean,
) : LifecycleOwner, LifecycleEventObserver {
    override val lifecycle = LifecycleRegistry(this)
    private var maxState = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED

    init {
        syncState()
    }

    /**
     * 更新标签页的前台激活状态，同步调整生命周期最大状态上限。
     */
    fun setActive(active: Boolean) {
        maxState = if (active) Lifecycle.State.RESUMED else Lifecycle.State.STARTED
        syncState()
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) = syncState()

    /**
     * 销毁生命周期注册表。
     */
    fun destroy() {
        lifecycle.currentState = Lifecycle.State.DESTROYED
    }

    /**
     * 将当前状态同步为父级生命周期与当前标签允许最大状态之间的较小值。
     */
    private fun syncState() {
        // Cached tabs keep their data current; backgrounding the app still stops collection.
        lifecycle.currentState = minOf(parent.lifecycle.currentState, maxState)
    }
}
