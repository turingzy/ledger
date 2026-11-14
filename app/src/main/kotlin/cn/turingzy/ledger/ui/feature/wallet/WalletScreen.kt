package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.ui.component.PressFeedback
import cn.turingzy.ledger.ui.component.TopBarIconButton
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.theme.IosYellow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import cn.turingzy.ledger.model.LedgerCardAppearance
import cn.turingzy.ledger.ui.component.CardBackgroundDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Stable
import cn.turingzy.ledger.ui.component.LocalBottomBarContentPadding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.ui.component.SummaryCard
import cn.turingzy.ledger.ui.component.SummaryCardContent
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.util.toFormattedAmount
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
        val account = success?.accounts?.firstOrNull { it.account.id == selected.id }?.account ?: selected
        AccountDetailsSheet(
            account = account,
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
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.padding(bottom = LocalBottomBarContentPadding.current)) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) { innerPadding ->
        when (uiState) {
            WalletUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                }
            }

            is WalletUiState.Success -> {
                val accounts = uiState.accounts
                var isListView by rememberSaveable { mutableStateOf(false) }
                var selectedAccountId by rememberSaveable { mutableStateOf<Long?>(null) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 0.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp + innerPadding.calculateBottomPadding() + LocalBottomBarContentPadding.current),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // 与账本页面顶栏设计保持一致: 大标题“钱包” + 右侧添加按钮 + 资产卡片 (卡片与顶栏间距 5.dp)
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
                                        text = "钱包",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = (-0.6).sp,
                                        modifier = Modifier.padding(bottom = 2.dp),
                                    )

                                    TopBarIconButton(
                                        imageVector = MaterialSymbols.AddCard,
                                        contentDescription = "添加账户",
                                        onClick = onAddAccountClick,
                                    )
                                }
                            }

                            WalletHeroCard(
                                totalAsset = uiState.totalAsset,
                                totalDebt = uiState.totalDebt,
                                appearance = cardAppearance,
                                background = cardBackground,
                                settingsEnabled = cardSettingsEnabled,
                                onUploadBackground = onUploadCardBackground,
                                onTransparencyChange = onCardTransparencyChange,
                            )
                        }
                    }

                    item(key = "accounts_section") {
                        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "账户",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                TopBarIconButton(
                                    imageVector = if (isListView) MaterialSymbols.ViewCarousel else MaterialSymbols.ViewAgenda,
                                    contentDescription = if (isListView) "切换为卡片堆叠视图" else "切换为纵向层叠视图",
                                    onClick = { isListView = !isListView },
                                )
                            }

                            if (isListView) {
                                VerticalBankCardDeck(
                                    accounts = accounts,
                                    onAdjustClick = onAdjustBalanceClick,
                                    selectedAccountId = selectedAccountId,
                                    onSelectedAccountChange = { selectedAccountId = it },
                                )
                            } else {
                                SwipeableBankCardDeck(
                                    accounts = accounts,
                                    onAdjustClick = onAdjustBalanceClick,
                                    selectedAccountId = selectedAccountId,
                                    onSelectedAccountChange = { selectedAccountId = it },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WalletHeroCard(
    totalAsset: Long,
    totalDebt: Long,
    modifier: Modifier = Modifier,
    appearance: LedgerCardAppearance = LedgerCardAppearance(),
    background: ImageBitmap? = null,
    settingsEnabled: Boolean = true,
    onUploadBackground: () -> Unit = {},
    onTransparencyChange: (Int) -> Unit = {},
) {
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
            title = "总资产",
            amount = totalAsset,
            amountsHidden = amountsHidden,
            onToggleAmounts = { amountsHidden = !amountsHidden },
            leftAmount = totalDebt,
            rightAmount = totalAsset + totalDebt,
            leftLabel = "总支出",
            rightLabel = "总收入",
        )
    }
}

internal fun accountCardColors(account: Account): List<Color> {
    val isCenturion = account.templateId == 2L || account.name.contains("百夫长") || account.name.contains("运通")
    return when {
        isCenturion -> listOf(Color(0xFF0D0E11), Color(0xFF141519), Color(0xFF08080A))
        account.templateId == 1L -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)) // 经典深蓝
        account.templateId == 3L -> listOf(Color(0xFF075E37), Color(0xFF03361E)) // 翡翠绿 (微信/活力薄荷)
        account.type == AccountType.CREDIT -> listOf(Color(0xFF2D143D), Color(0xFF1A0D26)) // 尊贵紫 (白金信用卡)
        account.type == AccountType.WALLET && account.name.contains("支付宝") -> listOf(Color(0xFF0A438C), Color(0xFF052148)) // 皇家科技蓝
        else -> listOf(Color(0xFF1C1C1E), Color(0xFF121214)) // 黑曜金 (金葵花/储蓄卡)
    }
}

@Composable
fun BankCardItem(
    account: Account,
    template: CardTemplate?,
    onAdjustClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDetails: Boolean = true,
    showBackPlaceholder: Boolean = false,
) {
    val pressColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.05f)
    val isCenturion = account.templateId == 2L || account.name.contains("百夫长") || account.name.contains("运通")

    val cardGradients = accountCardColors(account)
    val textColor = template?.takeUnless { it.isDeleted }?.textColor?.cardFaceTextColor(MaterialTheme.colorScheme.onPrimary)
        ?: MaterialTheme.colorScheme.onPrimary
    val cover = template?.takeUnless { it.isDeleted }?.coverUri?.let(android.net.Uri::parse)

    Card(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color.Black.copy(alpha = 0.35f),
                spotColor = Color.Black.copy(alpha = 0.35f),
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = remember(pressColor) { PressFeedback(pressColor) },
                onClick = onAdjustClick,
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardGradients.first()),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(cardGradients)),
        ) {
            if (cover?.scheme in setOf("content", "file", "android.resource")) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context -> android.widget.ImageView(context).apply { scaleType = android.widget.ImageView.ScaleType.CENTER_CROP } },
                    update = { image ->
                        if (image.tag != cover) { image.setImageURI(cover); image.tag = cover }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
            if (!showDetails) {
                if (showBackPlaceholder) Box(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) { BankCardBackPlaceholder() }
                return@Box
            }

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
                            fontSize = 12.sp,
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
                            fontSize = 14.sp,
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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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
                            fontSize = 13.sp,
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
private fun BankCardBackPlaceholder() {
    val white = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(width = 50.dp, height = 11.dp).clip(CircleShape).background(white.copy(alpha = 0.60f)))
            Box(Modifier.size(width = 34.dp, height = 5.dp).clip(CircleShape).background(white.copy(alpha = 0.35f)))
        }
        Box(Modifier.size(width = 52.dp, height = 10.dp).clip(CircleShape).background(white.copy(alpha = 0.45f)))
    }
}

@Composable
fun EmvChip(modifier: Modifier = Modifier) {
    Icon(
        imageVector = MaterialSymbols.Memory,
        contentDescription = null,
        modifier = modifier.size(width = 32.dp, height = 22.dp),
        tint = IosYellow,
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
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFFE60012)))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF003882)))
            Box(modifier = Modifier.weight(1f).fillMaxHeight().background(Color(0xFF008453)))
        }
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
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

@Preview(showBackground = true)
@Composable
fun WalletContentPreview() {
    MaterialTheme {
        WalletContent(
            uiState = WalletUiState.Success(
                totalAsset = 26115000L,
                totalDebt = 1400000L,
                accounts = listOf(
                    AccountWithTemplate(
                        account = Account(
                            id = 1L,
                            name = "美国运通 · 百夫长黑金卡",
                            type = AccountType.DEBIT,
                            initialBalance = 10000000L,
                            currentBalance = 11850000L,
                            cardNumber = "3759876543215288",
                            templateId = 2L,
                        ),
                        template = null,
                    ),
                    AccountWithTemplate(
                        account = Account(
                            id = 2L,
                            name = "微信支付",
                            type = AccountType.WALLET,
                            initialBalance = 200000L,
                            currentBalance = 1235000L,
                            cardNumber = "8820",
                            templateId = 3L,
                        ),
                        template = null,
                    ),
                    AccountWithTemplate(
                        account = Account(
                            id = 3L,
                            name = "招商银行 · 经典白金卡",
                            type = AccountType.CREDIT,
                            initialBalance = 0L,
                            currentBalance = 1400000L,
                            cardNumber = "4392258877619012",
                            templateId = 1L,
                        ),
                        template = null,
                    ),
                ),
            ),
            onAddAccountClick = {},
            onAdjustBalanceClick = {},
            onDeleteAccountClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TwoCardsDeckPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            SwipeableBankCardDeck(
                accounts = listOf(
                    AccountWithTemplate(
                        account = Account(
                            id = 1L,
                            name = "美国运通 · 百夫长黑金卡",
                            type = AccountType.DEBIT,
                            initialBalance = 10000000L,
                            currentBalance = 11850000L,
                            cardNumber = "3759876543215288",
                            templateId = 2L,
                        ),
                        template = null,
                    ),
                    AccountWithTemplate(
                        account = Account(
                            id = 2L,
                            name = "微信支付",
                            type = AccountType.WALLET,
                            initialBalance = 200000L,
                            currentBalance = 1235000L,
                            cardNumber = "8820",
                            templateId = 3L,
                        ),
                        template = null,
                    ),
                ),
                onAdjustClick = {},
            )
        }
    }
}
