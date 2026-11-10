package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import cn.turingzy.ledger.model.LedgerCardAppearance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.ui.component.CardBackgroundDialog
import cn.turingzy.ledger.ui.component.categoryGlyph
import cn.turingzy.ledger.ui.theme.IosYellow
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon as ModelIcon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.theme.IosSecondary
import cn.turingzy.ledger.util.toFormattedAmount
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    showRecordSheet: Boolean,
    onRecordSheetDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LedgerViewModel = hiltViewModel(),
    onNavigateToRecord: () -> Unit = {},
) {
    val cardViewModel: LedgerCardViewModel = hiltViewModel()
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
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    val recordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LedgerContent(
        uiState = uiState,
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
            defaultFilename = "bg_ledger_card.jpg",
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

    if (showRecordSheet) {
        RecordTransactionSheet(
            sheetState = recordSheetState,
            categories = categories,
            accounts = accounts,
            onDismissRequest = onRecordSheetDismiss,
            onConfirm = { type, amount, accountId, toAccountId, categoryId, note, transactionTime ->
                viewModel.createTransaction(type, amount, accountId, toAccountId, categoryId, note, transactionTime)
                scope.launch { recordSheetState.hide() }.invokeOnCompletion {
                    if (!recordSheetState.isVisible) {
                        onRecordSheetDismiss()
                    }
                }
            },
        )
    }
}

@Composable
fun LedgerContent(
    uiState: LedgerUiState,
    modifier: Modifier = Modifier,
    initialSearchActive: Boolean = false,
    cardAppearance: LedgerCardAppearance = LedgerCardAppearance(),
    cardBackground: ImageBitmap? = null,
    cardSettingsEnabled: Boolean = true,
    onUploadCardBackground: () -> Unit = {},
    onCardTransparencyChange: (Int) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var isSearchActive by rememberSaveable { mutableStateOf(initialSearchActive) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var submittedSearchQuery by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(240)
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) { innerPadding ->
        when (uiState) {
            LedgerUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                }
            }

            is LedgerUiState.Success -> {
                val filteredDayGroups = remember(uiState.dayGroups, submittedSearchQuery) {
                    if (submittedSearchQuery.isBlank()) {
                        uiState.dayGroups
                    } else {
                        uiState.dayGroups.mapNotNull { group ->
                            val matchedTxs = group.transactions.filter { tx ->
                                tx.transaction.note?.contains(submittedSearchQuery, ignoreCase = true) == true ||
                                    tx.category.category.name.contains(submittedSearchQuery, ignoreCase = true) ||
                                    tx.account.name.contains(submittedSearchQuery, ignoreCase = true)
                            }
                            if (matchedTxs.isNotEmpty()) {
                                group.copy(
                                    transactions = matchedTxs,
                                    totalExpense = matchedTxs.filter { it.transaction.type == TransactionType.EXPENSE }.sumOf { it.transaction.amount },
                                    totalIncome = matchedTxs.filter { it.transaction.type == TransactionType.INCOME }.sumOf { it.transaction.amount },
                                )
                            } else null
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 0.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                                // 默认顶栏: 大标题“账本” + 日期选择器 + 搜索图标按钮
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = !isSearchActive,
                                    enter = fadeIn(tween(durationMillis = 280, delayMillis = 60)),
                                    exit = fadeOut(tween(durationMillis = 180)),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = "账本",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            letterSpacing = (-0.6).sp,
                                            modifier = Modifier.align(Alignment.CenterStart),
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 64.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = "${uiState.month}月",
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }

                                        // 搜索按钮 (圆形点击水波纹反馈)
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(bounded = true, radius = 18.dp),
                                                ) {
                                                    isSearchActive = true
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = MaterialSymbols.Search,
                                                contentDescription = "搜索",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                }

                                // 展开的动态向左自然伸出、点击x向右优雅收缩的搜索框
                                androidx.compose.animation.AnimatedVisibility(
                                    visible = isSearchActive,
                                    enter = fadeIn(tween(durationMillis = 320)) + expandHorizontally(
                                        expandFrom = Alignment.End,
                                        animationSpec = tween(
                                            durationMillis = 360,
                                            easing = CubicBezierEasing(0.20f, 0.0f, 0.10f, 1.0f),
                                        ),
                                    ),
                                    exit = fadeOut(tween(durationMillis = 240)) + shrinkHorizontally(
                                        shrinkTowards = Alignment.End,
                                        animationSpec = tween(
                                            durationMillis = 320,
                                            easing = CubicBezierEasing(0.20f, 0.0f, 0.10f, 1.0f),
                                        ),
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(Color(0xFFE5E5EA))
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = MaterialSymbols.Search,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(24.dp),
                                        )

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            contentAlignment = Alignment.CenterStart,
                                        ) {
                                            BasicTextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                textStyle = TextStyle(
                                                    fontSize = 15.sp,
                                                    lineHeight = 22.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                ),
                                                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                                keyboardActions = KeyboardActions(
                                                    onSearch = {
                                                        submittedSearchQuery = searchQuery.trim()
                                                        focusManager.clearFocus()
                                                    },
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .focusRequester(focusRequester),
                                                decorationBox = { innerTextField ->
                                                    Box(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        contentAlignment = Alignment.CenterStart,
                                                    ) {
                                                        if (searchQuery.isEmpty()) {
                                                            Text(
                                                                text = "搜索账单备注、分类、账户",
                                                                fontSize = 15.sp,
                                                                lineHeight = 22.sp,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(start = 6.dp),
                                                            )
                                                        }
                                                        innerTextField()
                                                    }
                                                },
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(4.dp))

                                        // 点击 X 圆形水波纹触发效果 (大小与搜索图标一致 24dp，背景非常驻而是点击时触发)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .clickable(
                                                    interactionSource = remember { MutableInteractionSource() },
                                                    indication = ripple(bounded = true, radius = 18.dp),
                                                ) {
                                                    isSearchActive = false
                                                    searchQuery = ""
                                                    submittedSearchQuery = ""
                                                    focusManager.clearFocus()
                                                },
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                imageVector = MaterialSymbols.Close,
                                                contentDescription = "关闭搜索",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            // 月度结余 Hero 大卡片 (整体向上移动，与顶栏间距减半至 5.dp)
                            SummaryHeroBanner(
                                cardMonthDisplay = uiState.cardMonthDisplay,
                                balance = uiState.monthBalance,
                                expense = uiState.monthExpense,
                                income = uiState.monthIncome,
                                appearance = cardAppearance,
                                background = cardBackground,
                                settingsEnabled = cardSettingsEnabled,
                                onUploadBackground = onUploadCardBackground,
                                onTransparencyChange = onCardTransparencyChange,
                            )
                        }
                    }

                    if (filteredDayGroups.isEmpty()) {
                        item(key = "empty_placeholder") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = if (submittedSearchQuery.isNotBlank()) "未找到相关账单" else "本期暂无交易明细，点击右下角开始记账",
                                    fontSize = 14.sp,
                                    color = IosSecondary,
                                )
                            }
                        }
                    } else {
                        filteredDayGroups.forEach { dayGroup ->
                            item(key = "group_${dayGroup.dateEpochDay}") {
                                DayGroupSection(dayGroup = dayGroup)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayGroupSection(
    dayGroup: DayTransactionGroup,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dayGroup.displayDate,
                maxLines = 1,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
            )
            Text(
                text = "支出: ${dayGroup.totalExpense.toFormattedAmount(showSymbol = true)}  收入: ${dayGroup.totalIncome.toFormattedAmount(showSymbol = true)}",
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            dayGroup.transactions.forEach { detail ->
                key(detail.transaction.id) {
                    DayTransactionRowItem(detail = detail)
                }
            }
        }
    }
}

@Composable
fun DayTransactionRowItem(
    detail: TransactionDetail,
    modifier: Modifier = Modifier,
) {
    val tx = detail.transaction
    val colors = MaterialTheme.colorScheme
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val timeStr = remember(tx.transactionTime) {
        Instant.ofEpochMilli(tx.transactionTime).atZone(ZoneId.systemDefault()).format(timeFormatter)
    }
    val typeColor = when (tx.type) {
        TransactionType.EXPENSE -> colors.error
        TransactionType.INCOME -> colors.tertiary
        TransactionType.TRANSFER -> IosYellow
    }
    val amountText = when (tx.type) {
        TransactionType.EXPENSE -> "−${tx.amount.toFormattedAmount(showSymbol = true)}"
        TransactionType.INCOME -> "+${tx.amount.toFormattedAmount(showSymbol = true)}"
        TransactionType.TRANSFER -> tx.amount.toFormattedAmount(showSymbol = true)
    }
    val supportingText = listOfNotNull(detail.account.name, tx.note?.takeIf { it.isNotBlank() }).joinToString(" · ")
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier.fillMaxWidth()
            .clip(shape)
            .background(colors.surface)
            .border(1.dp, colors.outline.copy(alpha = colors.outline.alpha * 0.5f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(typeColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = detail.category.icon.categoryGlyph(),
                contentDescription = detail.category.category.name,
                tint = if (tx.type == TransactionType.TRANSFER) colors.scrim else colors.onPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = detail.category.category.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface,
                letterSpacing = (-0.2).sp,
            )
            Text(
                text = supportingText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                color = colors.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = amountText,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (tx.type == TransactionType.TRANSFER) colors.onSurface else typeColor,
                letterSpacing = (-0.3).sp,
            )
            Text(
                text = timeStr,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LedgerContentPreview() {
    MaterialTheme {
        LedgerContent(
            uiState = LedgerUiState.Success(
                year = 2026,
                month = 11,
                monthDisplay = "2026.11",
                cardMonthDisplay = "结余",
                monthIncome = 2400000L,
                monthExpense = 1155000L,
                monthBalance = 1245000L,
                dayGroups = listOf(
                    DayTransactionGroup(
                        dateEpochDay = 20000L,
                        displayDate = "今天 · 11.01 周日",
                        totalExpense = 1111L,
                        totalIncome = 1100L,
                        transactions = listOf(
                            TransactionDetail(
                                transaction = Transaction(
                                    id = 1L,
                                    type = TransactionType.INCOME,
                                    amount = 1100L,
                                    accountId = 1L,
                                    categoryId = 2L,
                                    transactionTime = System.currentTimeMillis(),
                                    note = "理财收益",
                                ),
                                account = Account(id = 1L, name = "支付宝", type = AccountType.WALLET),
                                category = CategoryWithIcon(
                                    category = Category(id = 2L, name = "兼职收入", type = CategoryType.INCOME, iconId = 2L),
                                    icon = ModelIcon(id = 2L, keyName = "ic_income", displayName = "收入", iconUri = "res://"),
                                ),
                            ),
                            TransactionDetail(
                                transaction = Transaction(
                                    id = 2L,
                                    type = TransactionType.EXPENSE,
                                    amount = 1111L,
                                    accountId = 2L,
                                    categoryId = 1L,
                                    transactionTime = System.currentTimeMillis() - 3600000L,
                                    note = "午餐便当",
                                ),
                                account = Account(id = 2L, name = "招商银行", type = AccountType.DEBIT),
                                category = CategoryWithIcon(
                                    category = Category(id = 1L, name = "餐饮食品", type = CategoryType.EXPENSE, iconId = 1L),
                                    icon = ModelIcon(id = 1L, keyName = "ic_food", displayName = "餐饮", iconUri = "res://"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LedgerContentSearchActivePreview() {
    MaterialTheme {
        LedgerContent(
            initialSearchActive = true,
            uiState = LedgerUiState.Success(
                year = 2026,
                month = 11,
                monthDisplay = "2026.11",
                cardMonthDisplay = "结余",
                monthIncome = 2400000L,
                monthExpense = 1155000L,
                monthBalance = 1245000L,
                dayGroups = emptyList(),
            ),
        )
    }
}
