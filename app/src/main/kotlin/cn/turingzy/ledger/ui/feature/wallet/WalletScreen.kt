package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.ui.feature.wallet.component.SwipeableBankCardDeck
import cn.turingzy.ledger.ui.feature.wallet.sheet.AccountDetailsSheet
import cn.turingzy.ledger.ui.feature.wallet.sheet.AddAccountSheet
import cn.turingzy.ledger.ui.component.base.ScrollTitleContainerBase
import androidx.compose.foundation.lazy.rememberLazyListState
import cn.turingzy.ledger.ui.component.card.summary.TopBarIconButton
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.PageFrameBase
import cn.turingzy.ledger.ui.interaction.rememberUiEvent
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.ui.component.card.summary.CardBackgroundDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Stable
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.ui.component.card.summary.SummaryCard
import cn.turingzy.ledger.ui.component.card.summary.SummaryCardContent
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.account.Account

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    val cardViewModel: WalletCardViewModel = hiltViewModel()
    val cardUiState by cardViewModel.uiState.collectAsStateWithLifecycle()
    val background = remember(cardUiState.background) { cardUiState.background?.asImageBitmap() }
    var showBackgroundDialog by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(cardUiState.error, showBackgroundDialog) {
        if (showBackgroundDialog) return@LaunchedEffect
        val error = cardUiState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        cardViewModel.clearError()
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val accountSheets = remember { WalletAccountSheetController() }

    WalletContent(
        uiState = uiState,
        onAddAccountClick = accountSheets::showAdd,
        onAdjustBalanceClick = accountSheets::showDetails,
        modifier = modifier,
        cardAppearance = cardUiState.appearance,
        cardBackground = background,
        cardSettingsEnabled = !cardUiState.loading && !cardUiState.importing,
        onUploadCardBackground = {
            cardViewModel.clearError()
            showBackgroundDialog = true
        },
        onCardTransparencyChange = cardViewModel::setTransparencyPercent,
        snackbarHostState = snackbarHostState,
    )

    if (showBackgroundDialog) {
        CardBackgroundDialog(
            defaultFilename = "bg_wallet_card.jpg",
            preparedFilename = cardUiState.preparedBackground,
            busy = cardUiState.importing,
            error = cardUiState.error,
            onDismiss = {
                cardViewModel.clearError()
                cardViewModel.discardPreparedBackground()
                showBackgroundDialog = false
            },
            onLocalImage = cardViewModel::prepareBackground,
            onConfirm = { restoreDefault ->
                cardViewModel.applyBackground(restoreDefault) { showBackgroundDialog = false }
            },
        )
    }

    WalletAccountSheets(
        controller = accountSheets,
        uiState = uiState,
        onCreate = { value ->
            viewModel.createAccount(
                name = value.name,
                type = value.type,
                initialBalance = value.initialBalance,
                cardNumber = value.cardNumber,
                includeInTotal = value.includeInTotal,
                currency = value.currency,
                templateId = value.templateId,
            )
        },
        onSave = viewModel::updateAccountInfo,
        onDelete = viewModel::deleteAccount,
    )
}

@Stable
private class WalletAccountSheetController {
    var adding by mutableStateOf(false)
        private set
    var selected by mutableStateOf<Account?>(null)
        private set

    fun showAdd() {
        selected = null
        adding = true
    }

    fun showDetails(account: Account) {
        adding = false
        selected = account
    }

    fun dismissAdd() { adding = false }
    fun dismissDetails() { selected = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletAccountSheets(
    controller: WalletAccountSheetController,
    uiState: WalletUiState,
    onCreate: (Account) -> Unit,
    onSave: (Account) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val success = uiState as? WalletUiState.Success

    if (controller.adding) {
        AddAccountSheet(
            templates = success?.templates.orEmpty(),
            onDismissRequest = controller::dismissAdd,
            onConfirm = { name, type, balance, cardNo, includeInTotal, currency, templateId ->
                onCreate(
                    Account(
                        name = name,
                        type = type,
                        initialBalance = balance,
                        currentBalance = balance,
                        cardNumber = cardNo,
                        includeInTotal = includeInTotal,
                        currency = currency,
                        templateId = templateId,
                    ),
                )
            },
        )
    }

    controller.selected?.let { selected ->
        val current = success?.accounts?.firstOrNull { it.account.id == selected.id }
        AccountDetailsSheet(
            account = current?.account ?: selected,
            assignedTemplate = current?.template,
            templates = success?.templates.orEmpty(),
            onDismissRequest = controller::dismissDetails,
            onSave = onSave,
            onDelete = onDelete,
        )
    }
}

@Composable
fun WalletContent(
    uiState: WalletUiState,
    onAddAccountClick: () -> Unit,
    onAdjustBalanceClick: (Account) -> Unit,
    modifier: Modifier = Modifier,
    @Suppress("UNUSED_PARAMETER") onDeleteAccountClick: (Long) -> Unit = {},
    cardAppearance: LedgerCardAppearance = LedgerCardAppearance(),
    cardBackground: ImageBitmap? = null,
    cardSettingsEnabled: Boolean = true,
    onUploadCardBackground: () -> Unit = {},
    onCardTransparencyChange: (Int) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val openAccount = rememberUiEvent(onAdjustBalanceClick, UiInteractionKind.Navigation)
    PageFrameBase(modifier) {
        when (uiState) {
            WalletUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                }
            }

            is WalletUiState.Success -> {
                val accounts = uiState.accounts
                val currencyTotals = uiState.currencyTotals.ifEmpty { listOf(WalletCurrencyTotals("CNY", assets = uiState.totalAsset)) }
                val totals = currencyTotals.first()
                var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }

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
                            start = LedgerStyle.spacing.extraLarge, end = LedgerStyle.spacing.extraLarge,
                            top = LedgerStyle.spacing.extraSmall,
                            bottom = LedgerStyle.spacing.block + LocalBottomBarContentPadding.current,
                        ),
                        verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.sectionGap),
                    ) {
                        item(key = "header_and_hero") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    contentAlignment = Alignment.CenterEnd,
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = textResources.getString(cn.turingzy.ledger.R.string.ui_wallet),
                                            fontSize = LedgerStyle.typography.screenTitle.fontSize,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            letterSpacing = (-0.6).sp,
                                            modifier = Modifier.padding(bottom = 2.dp),
                                        )

                                        TopBarIconButton(
                                            imageVector = MaterialSymbols.AddCard,
                                            contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_add_account),
                                            onClick = rememberUiAction(onAddAccountClick, UiInteractionKind.Navigation),
                                        )
                                    }
                                }

                                if (uiState.amountsAvailable) WalletHeroCard(
                                    totalAsset = totals.assets,
                                    totalExpense = totals.expense,
                                    totalIncome = totals.income,
                                    currency = totals.currency,
                                    appearance = cardAppearance,
                                    background = cardBackground,
                                    settingsEnabled = cardSettingsEnabled,
                                    onUploadBackground = onUploadCardBackground,
                                    onTransparencyChange = onCardTransparencyChange,
                                ) else Text(textResources.getString(cn.turingzy.ledger.R.string.ui_exchange_rate_needed_refresh_or_enter_a_rate), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        item(key = "accounts_section") {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(LedgerStyle.dimensions.compactIconButton)
                                        .padding(start = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = textResources.getString(cn.turingzy.ledger.R.string.ui_account),
                                        fontSize = LedgerStyle.typography.transactionTitle.fontSize,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                SwipeableBankCardDeck(
                                    accounts = accounts,
                                    templates = uiState.templates,
                                    onAdjustClick = openAccount,
                                    selectedAccountId = selectedAccountId,
                                    onSelectedAccountChange = { selectedAccountId = it },
                                )
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(
            snackbarHostState,
            Modifier.align(Alignment.BottomCenter).padding(bottom = LocalBottomBarContentPadding.current),
        )
    }
}

@Composable
fun WalletHeroCard(
    totalAsset: Long,
    totalExpense: Long,
    totalIncome: Long,
    modifier: Modifier = Modifier,
    appearance: LedgerCardAppearance = LedgerCardAppearance(),
    background: ImageBitmap? = null,
    settingsEnabled: Boolean = true,
    onUploadBackground: () -> Unit = {},
    onTransparencyChange: (Int) -> Unit = {},
    currency: String = "CNY",
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var amountsHidden by rememberSaveable { mutableStateOf(false) }
    SummaryCard(
        modifier = modifier,
        defaultBackgroundRes = R.drawable.bg_wallet_card,
        appearance = appearance,
        background = background,
        settingsEnabled = settingsEnabled,
        onUploadBackground = onUploadBackground,
        onTransparencyChange = onTransparencyChange,
    ) {
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
        )
    }
}
