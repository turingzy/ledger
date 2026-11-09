package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.snapTo
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountWithTemplate
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val BankCardAspectRatio = 1.586f
private const val MaxBackCards = 3
private val CardSnapSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

@Composable
fun SwipeableBankCardDeck(
    accounts: List<AccountWithTemplate>,
    onAdjustClick: (Account) -> Unit,
    modifier: Modifier = Modifier,
    selectedAccountId: Long? = null,
    onSelectedAccountChange: (Long) -> Unit = {},
) {
    BankCardDeck(accounts, onAdjustClick, Orientation.Horizontal, modifier, selectedAccountId, onSelectedAccountChange)
}

@Composable
fun VerticalBankCardDeck(
    accounts: List<AccountWithTemplate>,
    onAdjustClick: (Account) -> Unit,
    modifier: Modifier = Modifier,
    selectedAccountId: Long? = null,
    onSelectedAccountChange: (Long) -> Unit = {},
) {
    BankCardDeck(accounts, onAdjustClick, Orientation.Vertical, modifier, selectedAccountId, onSelectedAccountChange)
}

@Composable
private fun BankCardDeck(
    accounts: List<AccountWithTemplate>,
    onAdjustClick: (Account) -> Unit,
    orientation: Orientation,
    modifier: Modifier = Modifier,
    selectedAccountId: Long? = null,
    onSelectedAccountChange: (Long) -> Unit = {},
) {
    if (accounts.isEmpty()) {
        Box(modifier.fillMaxWidth().height(194.dp), contentAlignment = Alignment.Center) {
            Text("暂无卡片，点击右上角 '+' 添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val ids = accounts.map { it.account.id }
    val selectedIndex = ids.indexOf(selectedAccountId).coerceAtLeast(0)
    val state = remember(ids, orientation) { AnchoredDraggableState(selectedIndex) }
    val scope = rememberCoroutineScope()
    val vertical = orientation == Orientation.Vertical
    val firstPage = if (accounts.size > 2) -1 else 0
    val lastPage = if (accounts.size > 2) accounts.size else accounts.lastIndex
    val overscrollEffect = rememberOverscrollEffect()
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state,
        positionalThreshold = { distance -> distance * 0.3f },
        animationSpec = CardSnapSpec,
    )

    LaunchedEffect(state, ids) {
        snapshotFlow { state.settledValue }.collect { page ->
            val index = Math.floorMod(page, accounts.size)
            onSelectedAccountChange(ids[index])
            // Rebase only after settling so wrapping never interrupts the finger or the spring.
            if (page != index) state.snapTo(index)
        }
    }

    BoxWithConstraints(modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val heightReferenceInset = if (accounts.size > 1) 18.dp else 0.dp
        val peekWidth = if (accounts.size > 1) 10.dp else 0.dp
        val cardWidth = if (vertical) maxWidth else maxWidth - peekWidth * 2
        val cardHeight = (maxWidth - heightReferenceInset * 2) / BankCardAspectRatio
        val backCount = minOf(accounts.lastIndex, MaxBackCards)
        val stackStep = 44.dp
        val deckHeight = cardHeight + if (vertical) stackStep * backCount else 0.dp
        val cardWidthPx = with(density) { cardWidth.roundToPx() }
        val cardHeightPx = with(density) { cardHeight.roundToPx() }
        val peekPx = with(density) { peekWidth.toPx() }
        val stackStepPx = with(density) { stackStep.toPx() }
        val dragDistance = with(density) { (cardHeight * 0.75f).toPx() }
        val anchors = remember(ids, dragDistance) {
            DraggableAnchors { for (page in firstPage..lastPage) page at page * dragDistance }
        }
        SideEffect { state.updateAnchors(anchors) }

        fun position(): Float = state.offset.takeUnless { it.isNaN() }?.div(dragDistance) ?: selectedIndex.toFloat()

        // Only changes when a card enters/leaves the stack; motion itself runs in placement/layers.
        val pages by remember(state, vertical, ids, dragDistance) {
            derivedStateOf { visibleCardPages(accounts.size, position(), vertical) }
        }
        val frontPage by remember(state, vertical, ids, dragDistance) {
            derivedStateOf {
                val currentPosition = position()
                if (vertical) {
                    visibleCardPages(accounts.size, currentPosition, true).minBy {
                        verticalCardDepth(it, currentPosition, accounts.size, MaxBackCards)
                    }
                } else {
                    visibleCardPages(accounts.size, currentPosition, false).minBy { abs(it - currentPosition) }
                }
            }
        }

        fun select(page: Int) {
            scope.launch { state.animateTo(page.coerceIn(firstPage, lastPage), CardSnapSpec) }
        }

        Layout(
            modifier = Modifier.fillMaxWidth().height(deckHeight + 16.dp)
                .semantics {
                    stateDescription = "第 ${Math.floorMod(state.settledValue, accounts.size) + 1} 张，共 ${accounts.size} 张卡片"
                    customActions = buildList {
                        if (state.settledValue > firstPage) {
                            add(CustomAccessibilityAction("上一张卡片") { select(state.settledValue - 1); true })
                        }
                        if (state.settledValue < lastPage) {
                            add(CustomAccessibilityAction("下一张卡片") { select(state.settledValue + 1); true })
                        }
                    }
                }
                .then(if (overscrollEffect != null) Modifier.overscroll(overscrollEffect) else Modifier)
                .anchoredDraggable(
                    state = state,
                    orientation = orientation,
                    reverseDirection = true,
                    enabled = accounts.size > 1,
                    overscrollEffect = overscrollEffect,
                    flingBehavior = flingBehavior,
                ),
            content = {
                pages.forEach { page ->
                    val item = accounts[Math.floorMod(page, accounts.size)]
                    key(item.account.id) {
                        BankCardItem(
                            account = item.account,
                            template = item.template,
                            onAdjustClick = {
                                if (page == frontPage && !state.isAnimationRunning) {
                                    onAdjustClick(item.account)
                                } else {
                                    select(page)
                                }
                            },
                            modifier = Modifier.semantics { contentDescription = item.account.name },
                            showDetails = page == frontPage,
                            showBackPlaceholder = vertical && page != frontPage,
                        )
                    }
                }
            },
        ) { measurables, constraints ->
            val placeables = measurables.map { it.measure(Constraints.fixed(cardWidthPx, cardHeightPx)) }
            layout(constraints.maxWidth, constraints.maxHeight) {
                val currentPosition = position()
                placeables.forEachIndexed { index, placeable ->
                    val page = pages[index]
                    val distance = page - currentPosition
                    val depth = if (vertical) {
                        verticalCardDepth(page, currentPosition, accounts.size, MaxBackCards)
                    } else abs(distance)
                    placeable.placeWithLayer(
                        x = (constraints.maxWidth - cardWidthPx) / 2,
                        y = with(density) { 8.dp.roundToPx() },
                        zIndex = accounts.size - depth,
                    ) {
                        if (vertical) {
                            translationY = (backCount - depth) * stackStepPx
                        } else {
                            translationX = distance.coerceIn(-1f, 1f) * peekPx
                            scaleY = 1f - 0.16f * depth.coerceAtMost(1f)
                            alpha = ((1.5f - depth) * 2f).coerceIn(0f, 1f)
                        }
                    }
                }
            }
        }
    }
}
