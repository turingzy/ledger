package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.ui.feature.wallet.component.SwipeableBankCardDeck
import cn.turingzy.ledger.ui.feature.wallet.sheet.AccountDetailsSheet
import cn.turingzy.ledger.ui.feature.wallet.sheet.AddAccountSheet
import cn.turingzy.ledger.ui.feature.wallet.sheet.cardFaceTextColor
import cn.turingzy.ledger.ui.component.base.ScrollTitleContainer
import androidx.compose.foundation.lazy.rememberLazyListState
import cn.turingzy.ledger.ui.component.card.bank.accountCardColors
import cn.turingzy.ledger.ui.component.card.bank.AccountCardBackground
import cn.turingzy.ledger.ui.interaction.rememberUiIndication
import cn.turingzy.ledger.ui.component.card.summary.TopBarIconButton
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.BasePageFrame
import cn.turingzy.ledger.ui.interaction.rememberUiEvent
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.ui.component.card.summary.CardBackgroundDialog
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Stable
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.ui.component.card.summary.SummaryCard
import cn.turingzy.ledger.ui.component.card.summary.SummaryCardContent
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
import kotlinx.coroutines.launch

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
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val success = uiState as? WalletUiState.Success
    fun dismissAdd() {
        scope.launch {
            addSheetState.hide()
            if (!addSheetState.isVisible) controller.dismissAdd()
        }
    }

    if (controller.adding) {
        AddAccountSheet(
            sheetState = addSheetState,
            templates = success?.templates.orEmpty(),
            onDismissRequest = ::dismissAdd,
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
                dismissAdd()
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
    BasePageFrame(modifier) {
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
                ScrollTitleContainer(
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


@Composable
fun BankCardItem(
    account: Account,
    template: CardTemplate?,
    onAdjustClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pressColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = LedgerStyle.opacity.pressed)
    val isCenturion = account.templateId == 2L || account.name.contains("百夫长") || account.name.contains("运通")

    val cardGradients = accountCardColors(account)
    val textColor = template?.takeUnless { it.isDeleted }?.textColor?.cardFaceTextColor(MaterialTheme.colorScheme.onPrimary)
        ?: MaterialTheme.colorScheme.onPrimary

    Card(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = LedgerStyle.shapes.bankCard,
                ambientColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
                spotColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.35f),
            )
            .clip(LedgerStyle.shapes.bankCard)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberUiIndication(pressColor),
                onClick = onAdjustClick,
            ),
        shape = LedgerStyle.shapes.bankCard,
        colors = CardDefaults.cardColors(containerColor = cardGradients.first()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(cardGradients)),
        ) {
            AccountCardBackground(account, template, Modifier.fillMaxSize())

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                if (isCenturion) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "AMERICAN EXPRESS",
                            fontSize = LedgerStyle.typography.label.fontSize,
                            fontWeight = FontWeight.ExtraBold,
                            color = textColor,
                            letterSpacing = 2.sp,
                        )
                        Text(
                            text = "CENTURION",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor.copy(alpha = 0.65f),
                            letterSpacing = 1.sp,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = account.name,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontSize = LedgerStyle.typography.bodyMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                        )

                        if (account.type == AccountType.DEBIT || account.type == AccountType.CREDIT) {
                            ContactlessWaveIcon()
                        }
                    }
                }

                if (isCenturion) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = LedgerStyle.spacing.tiny),
                        contentAlignment = Alignment.Center,
                    ) {
                        CenturionSoldierBadge()
                    }
                } else if (account.type == AccountType.DEBIT || account.type == AccountType.CREDIT) {
                    EmvChip()
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    val cardNumber = account.cardNumber?.takeIf {
                        (account.type == AccountType.DEBIT || account.type == AccountType.CREDIT) && it.isNotBlank()
                    }
                    if (cardNumber != null) {
                        Text(
                            text = "•••• ${cardNumber.takeLast(4)}",
                            fontSize = LedgerStyle.typography.bodySmall.fontSize,
                            fontWeight = FontWeight.Medium,
                            color = textColor,
                            letterSpacing = 1.5.sp,
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (!isCenturion && (account.type == AccountType.DEBIT || account.type == AccountType.CREDIT)) {
                        UnionPayBadge()
                    }
                }
            }
        }
    }
}

@Composable
fun EmvChip(modifier: Modifier = Modifier) {
    Icon(
        imageVector = MaterialSymbols.Memory,
        contentDescription = null,
        modifier = modifier.size(width = 32.dp, height = 22.dp),
        tint = LedgerStyle.decorationColors.chip,
    )
}

/**
 * 银联 UnionPay 芯片徽章
 */
@Composable
fun UnionPayBadge(
    modifier: Modifier = Modifier,
    label: String = "银联",
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(
            modifier = Modifier
                .size(width = 15.dp, height = 11.dp)
                .clip(RoundedCornerShape(2.dp)),
        ) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(LedgerStyle.decorationColors.unionPayRed))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(LedgerStyle.decorationColors.unionPayBlue))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(LedgerStyle.decorationColors.unionPayGreen))
        }
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = LedgerStyle.decorationColors.onBrand,
        )
    }
}

@Composable
fun ContactlessWaveIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = MaterialSymbols.Contactless,
        contentDescription = null,
        modifier = modifier.size(18.dp),
        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
    )
}

@Composable
fun CenturionSoldierBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.scrim),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MaterialSymbols.ShieldPerson,
            contentDescription = null,
            modifier = Modifier.size(30.dp),
            tint = MaterialTheme.colorScheme.onPrimary,
        )
    }
}
