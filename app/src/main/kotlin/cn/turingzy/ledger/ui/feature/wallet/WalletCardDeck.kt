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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.rememberUpdatedState
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
import cn.turingzy.ledger.ui.component.BankCardAspectRatio
import cn.turingzy.ledger.ui.component.CardPageIndicator
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountWithTemplate
import kotlinx.coroutines.launch

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
    if (accounts.isEmpty()) {
        EmptyCardDeck(modifier)
        return
    }
    val ids = accounts.map { it.account.id }
    val selectedIndex = ids.indexOf(selectedAccountId).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = selectedIndex, pageCount = { accounts.size })
    val onSelectedChange by rememberUpdatedState(onSelectedAccountChange)

    LaunchedEffect(pagerState, ids) {
        pagerState.scrollToPage(selectedIndex)
        snapshotFlow { pagerState.settledPage }.collect { page ->
            ids.getOrNull(page)?.let(onSelectedChange)
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(
            state = pagerState,
            key = { ids[it] },
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(vertical = 8.dp),
        ) { page ->
            val item = accounts[page]
            BankCardItem(
                account = item.account,
                template = item.template,
                onAdjustClick = { onAdjustClick(item.account) },
                modifier = Modifier.fillMaxWidth().aspectRatio(BankCardAspectRatio)
                    .semantics { contentDescription = item.account.name },
            )
        }
        CardPageIndicator(pageCount = accounts.size, currentPage = pagerState.currentPage)
    }
}

@Composable
fun VerticalBankCardDeck(
    accounts: List<AccountWithTemplate>,
    onAdjustClick: (Account) -> Unit,
    modifier: Modifier = Modifier,
    selectedAccountId: Long? = null,
    onSelectedAccountChange: (Long) -> Unit = {},
) {
    BankCardDeck(accounts, onAdjustClick, modifier, selectedAccountId, onSelectedAccountChange)
}

@Composable
private fun BankCardDeck(
    accounts: List<AccountWithTemplate>,
    onAdjustClick: (Account) -> Unit,
    modifier: Modifier = Modifier,
    selectedAccountId: Long? = null,
    onSelectedAccountChange: (Long) -> Unit = {},
) {
    if (accounts.isEmpty()) {
        EmptyCardDeck(modifier)
        return
    }

    val ids = accounts.map { it.account.id }
    val selectedIndex = ids.indexOf(selectedAccountId).coerceAtLeast(0)
    val state = remember(ids) { AnchoredDraggableState(selectedIndex) }
    val scope = rememberCoroutineScope()
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
        val cardWidth = maxWidth
        val cardHeight = cardWidth / BankCardAspectRatio
        val backCount = minOf(accounts.lastIndex, MaxBackCards)
        val stackStep = 44.dp
        val deckHeight = cardHeight + stackStep * backCount
        val cardWidthPx = with(density) { cardWidth.roundToPx() }
        val cardHeightPx = with(density) { cardHeight.roundToPx() }
        val stackStepPx = with(density) { stackStep.toPx() }
        val dragDistance = with(density) { (cardHeight * 0.75f).toPx() }
        val anchors = remember(ids, dragDistance) {
            DraggableAnchors { for (page in firstPage..lastPage) page at page * dragDistance }
        }
        SideEffect { state.updateAnchors(anchors) }

        fun position(): Float = state.offset.takeUnless { it.isNaN() }?.div(dragDistance) ?: selectedIndex.toFloat()

        // Only changes when a card enters/leaves the stack; motion itself runs in placement/layers.
        val pages by remember(state, ids, dragDistance) {
            derivedStateOf { visibleCardPages(accounts.size, position(), vertical = true) }
        }
        val frontPage by remember(state, ids, dragDistance) {
            derivedStateOf {
                val currentPosition = position()
                visibleCardPages(accounts.size, currentPosition, vertical = true).minBy {
                    verticalCardDepth(it, currentPosition, accounts.size, MaxBackCards)
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
                    orientation = Orientation.Vertical,
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
                            showBackPlaceholder = page != frontPage,
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
                    val depth = verticalCardDepth(page, currentPosition, accounts.size, MaxBackCards)
                    placeable.placeWithLayer(
                        x = (constraints.maxWidth - cardWidthPx) / 2,
                        y = with(density) { 8.dp.roundToPx() },
                        zIndex = accounts.size - depth,
                    ) {
                        translationY = (backCount - depth) * stackStepPx
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCardDeck(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().aspectRatio(BankCardAspectRatio), contentAlignment = Alignment.Center) {
        Text("暂无卡片，点击右上角 '+' 添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
