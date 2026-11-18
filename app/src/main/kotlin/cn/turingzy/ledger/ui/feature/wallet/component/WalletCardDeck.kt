package cn.turingzy.ledger.ui.feature.wallet.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.card.bank.BankCardAspectRatio
import cn.turingzy.ledger.ui.component.card.bank.rememberCircularCardPagerState
import cn.turingzy.ledger.ui.component.card.bank.CardPageIndicator
import cn.turingzy.ledger.ui.component.base.BaseEmptyState
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.resolveBuiltInAppearance

@Composable
fun SwipeableBankCardDeck(
    accounts: List<AccountWithTemplate>,
    onAdjustClick: (Account) -> Unit,
    modifier: Modifier = Modifier,
    selectedAccountId: Long? = null,
    onSelectedAccountChange: (Long) -> Unit = {},
    templates: List<CardTemplate> = emptyList(),
) {
    if (accounts.isEmpty()) {
        EmptyCardDeck(modifier)
        return
    }
    val ids = accounts.map { it.account.id }
    val pagerState = rememberCircularCardPagerState(ids, selectedAccountId)
    val onSelectedChange by rememberUpdatedState(onSelectedAccountChange)

    LaunchedEffect(pagerState, ids) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            onSelectedChange(ids[Math.floorMod(page, ids.size)])
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val settledPage = pagerState.settledPage
        HorizontalPager(
            state = pagerState,
            key = { it },
            pageSpacing = 12.dp,
            contentPadding = PaddingValues(bottom = 8.dp),
        ) { page ->
            val item = accounts[Math.floorMod(page, accounts.size)]
            BankCardItem(
                account = item.account,
                template = item.template.resolveBuiltInAppearance(templates),
                onAdjustClick = { onAdjustClick(item.account) },
                motionEnabled = page == settledPage,
                modifier = Modifier.fillMaxWidth()
                    .semantics { contentDescription = item.account.name },
            )
        }
        CardPageIndicator(pageCount = accounts.size, currentPage = Math.floorMod(pagerState.currentPage, accounts.size))
    }
}

@Composable
private fun EmptyCardDeck(modifier: Modifier = Modifier) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    Box(modifier.fillMaxWidth().aspectRatio(BankCardAspectRatio), contentAlignment = Alignment.Center) {
        BaseEmptyState(
            title = textResources.getString(cn.turingzy.ledger.R.string.ui_no_accounts),
            contentPadding = PaddingValues(LedgerStyle.spacing.none),
            titleStyle = MaterialTheme.typography.bodyLarge,
            titleColor = LedgerStyle.colors.textSecondary,
        )
    }
}
