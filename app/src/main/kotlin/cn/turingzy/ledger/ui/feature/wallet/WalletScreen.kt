package cn.turingzy.ledger.ui.feature.wallet

import android.net.Uri
import cn.turingzy.ledger.ui.feature.wallet.component.SwipeableBankCardDeck
import cn.turingzy.ledger.ui.feature.wallet.component.WalletAccountInfo
import cn.turingzy.ledger.ui.feature.wallet.sheet.AddAccountSheet
import cn.turingzy.ledger.ui.component.base.ScrollTitleContainerBase
import androidx.compose.foundation.lazy.rememberLazyListState
import cn.turingzy.ledger.ui.component.card.summary.TopBarIconButton
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.PageFrameBase
import cn.turingzy.ledger.ui.component.base.PrimaryPageHeaderBase
import cn.turingzy.ledger.ui.component.biz.MissingExchangeRateNoticeBiz
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.ui.component.card.summary.SummaryCard
import cn.turingzy.ledger.ui.component.card.summary.SummaryCardContent
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.account.Account

/**
 * 钱包页面根 Composable。
 *
 * 负责收集钱包数据状态及卡片外观状态，并协调账户新增、详情/修改、删除及背景设置等操作。
 *
 * @param modifier 外部修饰符
 * @param viewModel 钱包业务逻辑 ViewModel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    onOpenAccount: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    val cardViewModel: WalletCardViewModel = hiltViewModel()
    val cardUiState by cardViewModel.uiState.collectAsStateWithLifecycle()
    val background = remember(cardUiState.background) { cardUiState.background?.asImageBitmap() }
    val snackbarHostState = remember { SnackbarHostState() }
    val accountSaveError = stringResource(R.string.wallet_account_save_failed)
    LaunchedEffect(viewModel, accountSaveError) {
        viewModel.accountSaveErrors.collect { snackbarHostState.showSnackbar(accountSaveError) }
    }
    LaunchedEffect(cardUiState.error) {
        val error = cardUiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        cardViewModel.clearError()
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var addingAccount by rememberSaveable { mutableStateOf(false) }

    WalletContent(
        uiState = uiState,
        onAddAccountClick = { addingAccount = true },
        onOpenAccount = { onOpenAccount(it.id) },
        onUpdateAccount = viewModel::updateAccountInfo,
        onDeleteAccount = viewModel::deleteAccount,
        modifier = modifier,
        cardBackground = background,
        cardSettingsEnabled = !cardUiState.loading && !cardUiState.importing,
        onCardBackgroundSelected = cardViewModel::importBackground,
        snackbarHostState = snackbarHostState,
    )

    if (addingAccount) {
        AddAccountSheet(
            templates = (uiState as? WalletUiState.Success)?.templates.orEmpty(),
            onDismissRequest = { addingAccount = false },
            onConfirm = viewModel::createAccount,
        )
    }
}

/**
 * 钱包页面主内容区域。
 *
 * 展示顶部标题、新增账户入口、资产汇总卡片以及账户卡片叠放列表。
 *
 * @param uiState 钱包页面状态
 * @param onAddAccountClick 点击新增账户回调
 * @param onOpenAccount 点击轮播主卡时前往账户流水子页。
 * @param modifier 外部修饰符
 * @param cardBackground 汇总卡片自定义背景图
 * @param cardSettingsEnabled 是否允许修改卡片设置
 * @param onCardBackgroundSelected 选中卡片背景图片后的回调。
 * @param snackbarHostState 轻量消息提示状态
 */
@Composable
fun WalletContent(
    uiState: WalletUiState,
    onAddAccountClick: () -> Unit,
    onOpenAccount: (Account) -> Unit,
    modifier: Modifier = Modifier,
    cardBackground: ImageBitmap? = null,
    cardSettingsEnabled: Boolean = true,
    onCardBackgroundSelected: (Uri) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onUpdateAccount: (Account) -> Unit = {},
    onDeleteAccount: (Long) -> Unit = {},
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val bottomBarPadding = LocalBottomBarContentPadding.current
    PageFrameBase(modifier) {
        when (uiState) {
            WalletUiState.Loading -> Unit
            is WalletUiState.Success -> {
                val accounts = uiState.accounts
                val currencyTotals = uiState.currencyTotals.ifEmpty { listOf(WalletCurrencyTotals("CNY", assets = uiState.totalAsset)) }
                val totals = currencyTotals.first()
                var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }
                val selectedAccount = accounts.firstOrNull { it.account.id == selectedAccountId }
                    ?: accounts.firstOrNull()

                val listState = rememberLazyListState()
                ScrollTitleContainerBase(
                    title = textResources.getString(cn.turingzy.ledger.R.string.ui_wallet),
                    scrollOffset = {
                        if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset
                        else Int.MAX_VALUE
                    },
                    onScrollToTop = { listState.animateScrollToItem(0) },
                ) { scrollModifier ->
                    LazyColumn(
                        state = listState,
                        modifier = scrollModifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = LedgerStyle.spacing.pageHorizontal, end = LedgerStyle.spacing.pageHorizontal,
                            top = LedgerStyle.spacing.extraSmall,
                            bottom = LedgerStyle.spacing.block + bottomBarPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.sectionGap),
                    ) {
                        item(key = "header_and_hero") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.labelGap),
                            ) {
                                PrimaryPageHeaderBase(title = textResources.getString(R.string.ui_wallet)) {
                                    TopBarIconButton(
                                        imageVector = MaterialSymbols.AddCard,
                                        contentDescription = textResources.getString(R.string.ui_add_account),
                                        onClick = rememberUiAction(onAddAccountClick, UiInteractionKind.Navigation),
                                    )
                                }

                                if (uiState.amountsAvailable) WalletHeroCard(
                                    totalAsset = totals.assets,
                                    totalExpense = totals.expense,
                                    totalIncome = totals.income,
                                    currency = totals.currency,
                                    background = cardBackground,
                                    settingsEnabled = cardSettingsEnabled,
                                    onBackgroundSelected = onCardBackgroundSelected,
                                ) else MissingExchangeRateNoticeBiz()
                            }
                        }

                        item(key = "accounts_section") {
                            Column(verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.labelGap)) {
                                WalletSectionHeading(stringResource(R.string.ui_account))

                                SwipeableBankCardDeck(
                                    accounts = accounts,
                                    templates = uiState.templates,
                                    selectedAccountId = selectedAccountId,
                                    onSelectedAccountChange = { selectedAccountId = it },
                                    onOpenAccount = onOpenAccount,
                                )
                                if (selectedAccount != null) {
                                    WalletAccountInfo(selectedAccount, uiState.templates, onUpdateAccount, onDeleteAccount)
                                }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(
            snackbarHostState,
            Modifier.align(Alignment.BottomCenter).padding(bottom = bottomBarPadding),
        )
    }
}

/** 钱包主页面和账户子页共用的区块标题。 */
@Composable
internal fun WalletSectionHeading(title: String) {
    Row(
        Modifier.fillMaxWidth().height(LedgerStyle.dimensions.compactIconButton).padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = LedgerStyle.typography.transactionTitle.fontSize,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 钱包顶部总资产概览卡片。
 *
 * 显示总资产、总支出与总收入，支持隐藏金额和自定义卡片背景图片。
 *
 * @param totalAsset 总资产金额（单位：分）
 * @param totalExpense 总支出金额（单位：分）
 * @param totalIncome 总收入金额（单位：分）
 * @param modifier 外部修饰符
 * @param background 卡片自定义背景位图
 * @param settingsEnabled 是否启用卡片背景及外观设置
 * @param onBackgroundSelected 选中卡片背景图片后的回调。
 * @param currency 币种代码，默认为 "CNY"
 */
@Composable
fun WalletHeroCard(
    totalAsset: Long,
    totalExpense: Long,
    totalIncome: Long,
    modifier: Modifier = Modifier,
    background: ImageBitmap? = null,
    settingsEnabled: Boolean = true,
    onBackgroundSelected: (Uri) -> Unit = {},
    currency: String = "CNY",
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var amountsHidden by rememberSaveable { mutableStateOf(false) }
    SummaryCard(
        modifier = modifier,
        defaultBackgroundRes = R.drawable.bg_wallet_card,
        background = background,
        settingsEnabled = settingsEnabled,
        onBackgroundSelected = onBackgroundSelected,
    ) { activeFace ->
        key(currency) {
            SummaryCardContent(
                title = textResources.getString(cn.turingzy.ledger.R.string.ui_total_assets),
                amount = totalAsset,
                amountsHidden = amountsHidden,
                onToggleAmounts = { amountsHidden = !amountsHidden },
                leftAmount = totalExpense,
                rightAmount = totalIncome,
                currency = currency,
                leftLabel = textResources.getString(cn.turingzy.ledger.R.string.ui_total_expenses),
                rightLabel = textResources.getString(cn.turingzy.ledger.R.string.ui_total_income),
                activeFace = activeFace,
            )
        }
    }
}
