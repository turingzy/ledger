package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.ui.feature.wallet.component.SwipeableBankCardDeck
import cn.turingzy.ledger.ui.feature.wallet.sheet.AccountDetailsSheet
import cn.turingzy.ledger.ui.feature.wallet.sheet.AddAccountSheet
import cn.turingzy.ledger.ui.component.base.ScrollTitleContainerBase
import androidx.compose.foundation.lazy.rememberLazyListState
import cn.turingzy.ledger.ui.component.navigation.LedgerFloatingActionButton
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.PageFrameBase
import cn.turingzy.ledger.ui.component.base.PrimaryPageHeaderBase
import cn.turingzy.ledger.ui.interaction.rememberUiEvent
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
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.ui.component.card.summary.rememberSummaryBackgroundPicker
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
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    val cardViewModel: WalletCardViewModel = hiltViewModel()
    val cardUiState by cardViewModel.uiState.collectAsStateWithLifecycle()
    val background = remember(cardUiState.background) { cardUiState.background?.asImageBitmap() }
    val pickBackground = rememberSummaryBackgroundPicker(cardViewModel::importBackground)
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(cardUiState.error) {
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
        onUploadCardBackground = pickBackground,
        onCardTransparencyChange = cardViewModel::setTransparencyPercent,
        snackbarHostState = snackbarHostState,
    )

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

/**
 * 钱包页面账户底部弹窗（新增账户 / 账户详情）状态控制器。
 */
@Stable
private class WalletAccountSheetController {
    /** 是否正在展示新增账户弹窗 */
    var adding by mutableStateOf(false)
        private set
    /** 当前选中的账户，非空时展示账户详情弹窗 */
    var selected by mutableStateOf<Account?>(null)
        private set

    /** 显示新增账户弹窗 */
    fun showAdd() {
        selected = null
        adding = true
    }

    /**
     * 显示指定账户的详情与编辑弹窗
     *
     * @param account 目标账户
     */
    fun showDetails(account: Account) {
        adding = false
        selected = account
    }

    /** 关闭新增账户弹窗 */
    fun dismissAdd() { adding = false }
    /** 关闭账户详情弹窗 */
    fun dismissDetails() { selected = null }
}

/**
 * 钱包账户底部弹窗容器，根据控制器状态渲染新增账户或账户详情弹窗。
 *
 * @param controller 弹窗状态控制器
 * @param uiState 当前钱包 UI 状态
 * @param onCreate 创建新账户回调
 * @param onSave 保存账户修改回调
 * @param onDelete 删除账户回调
 */
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

/**
 * 钱包页面主内容区域。
 *
 * 展示顶部标题、新增账户入口、资产汇总卡片以及账户卡片叠放列表。
 *
 * @param uiState 钱包页面状态
 * @param onAddAccountClick 点击新增账户回调
 * @param onAdjustBalanceClick 点击调整账户余额/查看账户详情回调
 * @param modifier 外部修饰符
 * @param onDeleteAccountClick 删除账户回调（保留参数）
 * @param cardAppearance 汇总卡片外观样式
 * @param cardBackground 汇总卡片自定义背景图
 * @param cardSettingsEnabled 是否允许修改卡片设置
 * @param onUploadCardBackground 上传卡片背景图片回调
 * @param onCardTransparencyChange 调整卡片透明度回调
 * @param snackbarHostState 轻量消息提示状态
 */
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
            WalletUiState.Loading -> Unit
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
                            start = LedgerStyle.spacing.pageHorizontal, end = LedgerStyle.spacing.pageHorizontal,
                            top = LedgerStyle.spacing.extraSmall,
                            bottom = LedgerStyle.spacing.block + LocalBottomBarContentPadding.current + LedgerStyle.dimensions.touchTarget,
                        ),
                        verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.sectionGap),
                    ) {
                        item(key = "header_and_hero") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.labelGap),
                            ) {
                                PrimaryPageHeaderBase(title = textResources.getString(R.string.ui_wallet))

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
                            Column(verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.labelGap)) {
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
        val bottomPadding = LocalBottomBarContentPadding.current
        LedgerFloatingActionButton(
            label = textResources.getString(R.string.ui_account),
            icon = MaterialSymbols.AddCard,
            visible = uiState is WalletUiState.Success,
            onClick = onAddAccountClick,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = bottomPadding),
        )
        SnackbarHost(
            snackbarHostState,
            Modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding + LedgerStyle.dimensions.touchTarget),
        )
    }
}

/**
 * 钱包顶部总资产概览卡片。
 *
 * 显示总资产、总支出与总收入，支持隐藏金额以及卡片背景与透明度自定义。
 *
 * @param totalAsset 总资产金额（单位：分）
 * @param totalExpense 总支出金额（单位：分）
 * @param totalIncome 总收入金额（单位：分）
 * @param modifier 外部修饰符
 * @param appearance 卡片外观样式
 * @param background 卡片自定义背景位图
 * @param settingsEnabled 是否启用卡片背景及外观设置
 * @param onUploadBackground 触发背景图片选择回调
 * @param onTransparencyChange 修改背景透明度百分比回调
 * @param currency 币种代码，默认为 "CNY"
 */
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
