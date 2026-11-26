package cn.turingzy.ledger.ui.feature.wallet.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import cn.turingzy.ledger.ui.component.card.bank.BankCardAspectRatio
import cn.turingzy.ledger.ui.component.card.bank.rememberCircularCardPagerState
import cn.turingzy.ledger.ui.component.card.bank.TitaniumBankCard
import cn.turingzy.ledger.ui.component.card.bank.TitaniumDrag
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.base.CenteredEmptyStateBase
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.resolveBuiltInAppearance
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 可左右滑动的银行卡卡叠组件。
 *
 * 单账户全宽展示，多账户循环滑动，靠左右露出的卡边提示相邻账户。
 *
 * @param accounts 账户及其模板列表
 * @param modifier 外部修饰符
 * @param selectedAccountId 当前选中的账户 ID
 * @param onSelectedAccountChange 选中账户变更回调
 * @param templates 预置或自定义卡面模板列表
 */
@Composable
fun SwipeableBankCardDeck(
    accounts: List<AccountWithTemplate>,
    modifier: Modifier = Modifier,
    selectedAccountId: Long? = null,
    onSelectedAccountChange: (Long) -> Unit = {},
    templates: List<CardTemplate> = emptyList(),
    onOpenAccount: ((Account) -> Unit)? = null,
) {
    if (accounts.isEmpty()) {
        EmptyCardDeck(modifier)
        return
    }
    val onSelectedChange by rememberUpdatedState(onSelectedAccountChange)
    if (accounts.size == 1) {
        val item = accounts.single()
        LaunchedEffect(item.account.id) { onSelectedChange(item.account.id) }
        WalletAccountCard(item, templates, modifier.padding(bottom = 8.dp), active = true, onOpenAccount = onOpenAccount)
        return
    }

    val ids = accounts.map { it.account.id }
    val pagerState = rememberCircularCardPagerState(ids, selectedAccountId)
    val backgroundScale = 0.84f
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1f else -1f

    LaunchedEffect(pagerState, ids) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            onSelectedChange(ids[Math.floorMod(page, ids.size)])
        }
    }
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val edgeWidth = minOf(16.dp, maxWidth / 4)
        val edgePixels = with(LocalDensity.current) { edgeWidth.roundToPx() }
        // 露边只减窄主卡，卡叠沿用全宽单卡在同一像素密度下的高度。
        val cardWidth = (constraints.maxWidth - 2 * edgePixels).coerceAtLeast(1)
        val singleHeight = (constraints.maxWidth / BankCardAspectRatio).roundToInt().coerceAtLeast(1)
        val stackAspectRatio = cardWidth.toFloat() / singleHeight
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            key = { it },
            beyondViewportPageCount = 1,
            contentPadding = PaddingValues(start = edgeWidth, end = edgeWidth, bottom = 8.dp),
        ) { page ->
            WalletAccountCard(
                item = accounts[Math.floorMod(page, accounts.size)],
                templates = templates,
                modifier = Modifier.zIndex(-abs(page - pagerState.currentPage).toFloat()).graphicsLayer {
                    val offset = pagerState.getOffsetDistanceInPages(page)
                    val depth = abs(offset)
                    val scale = lerp(1f, backgroundScale, depth.coerceAtMost(1f))
                    scaleX = scale
                    scaleY = scale
                    alpha = (2f - depth).coerceIn(0f, 1f)

                    // 保留原分页的滑动距离，只把绘制位置向中心压叠，补偿缩放后的边缘内缩。
                    val pageWidth = pagerState.layoutInfo.pageSize.toFloat()
                    val stackSpacing = edgePixels + pageWidth * (1f - backgroundScale) / 2f
                    translationX = offset * (stackSpacing - pageWidth) * direction
                },
                active = page == pagerState.settledPage,
                aspectRatio = stackAspectRatio,
                onOpenAccount = onOpenAccount,
            )
        }
    }
}

/** 只朗读当前卡片的账户名，左右背景卡仅作视觉提示。 */
@Composable
private fun WalletAccountCard(
    item: AccountWithTemplate,
    templates: List<CardTemplate>,
    modifier: Modifier,
    active: Boolean,
    aspectRatio: Float = BankCardAspectRatio,
    onOpenAccount: ((Account) -> Unit)?,
) {
    val openModifier = if (active && onOpenAccount != null) {
        Modifier.clickable(interactionSource = null, indication = null, role = Role.Button) { onOpenAccount(item.account) }
    } else Modifier
    TitaniumBankCard(
        template = item.template.resolveBuiltInAppearance(templates),
        motionEnabled = active,
        drag = TitaniumDrag.Disabled,
        aspectRatio = aspectRatio,
        modifier = modifier.fillMaxWidth().then(openModifier).then(
            if (active) Modifier.semantics { contentDescription = item.account.name }
            else Modifier.clearAndSetSemantics { hideFromAccessibility() },
        ),
    )
}

/**
 * 银行卡卡叠为空时的占位展示组件。
 *
 * 保持与银行卡相同的宽高比并显示空状态文案。
 *
 * @param modifier 外部修饰符
 */
@Composable
private fun EmptyCardDeck(modifier: Modifier = Modifier) {
    CenteredEmptyStateBase(
        title = stringResource(R.string.ui_no_accounts),
        modifier = modifier.fillMaxWidth().aspectRatio(BankCardAspectRatio),
    )
}
