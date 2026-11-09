package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DirectionsSubway
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.ui.component.CardBackgroundDialog
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon as ModelIcon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.theme.IosBlue
import cn.turingzy.ledger.ui.theme.IosGreen
import cn.turingzy.ledger.ui.theme.IosOrange
import cn.turingzy.ledger.ui.theme.IosPurple
import cn.turingzy.ledger.ui.theme.IosRed
import cn.turingzy.ledger.ui.theme.IosSecondary
import cn.turingzy.ledger.util.toFormattedAmount
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import cn.turingzy.ledger.ui.component.calendar.CalendarPickerState
import cn.turingzy.ledger.ui.component.calendar.CalendarSelection
import cn.turingzy.ledger.ui.component.calendar.CalendarRangeSelector
import cn.turingzy.ledger.ui.component.calendar.MonthCalendarDatePickerCard
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

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
    LaunchedEffect(cardUiState.error) {
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
        onSelectWeek = { anchor -> viewModel.setWeek(anchor) },
        onSelectMonth = { year, month -> viewModel.setMonth(year, month) },
        onSelectYear = { year -> viewModel.setYear(year) },
        onSelectRange = { start, end -> viewModel.setRange(start, end) },
        modifier = modifier,
        cardAppearance = cardUiState.appearance,
        cardBackground = background,
        cardSettingsEnabled = !cardUiState.loading && !cardUiState.importing,
        onUploadCardBackground = { showBackgroundDialog = true },
        onCardTransparencyEnabledChange = cardViewModel::setTransparencyEnabled,
        onCardTransparencyChange = cardViewModel::setTransparencyPercent,
        snackbarHostState = snackbarHostState,
    )

    if (showBackgroundDialog) {
        CardBackgroundDialog(
            onDismiss = { showBackgroundDialog = false },
            onRestoreDefault = {
                showBackgroundDialog = false
                cardViewModel.resetBackground()
            },
            onLocalImage = { uri ->
                showBackgroundDialog = false
                cardViewModel.importBackground(uri)
            },
            onImageUrl = { url ->
                showBackgroundDialog = false
                cardViewModel.importBackgroundUrl(url)
            },
        )
    }

    if (showRecordSheet) {
        RecordTransactionSheet(
            sheetState = recordSheetState,
            categories = categories,
            accounts = accounts,
            onDismissRequest = onRecordSheetDismiss,
            onConfirm = { type, amount, accountId, toAccountId, categoryId, note ->
                viewModel.createTransaction(type, amount, accountId, toAccountId, categoryId, note)
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
    onSelectWeek: (LocalDate) -> Unit,
    onSelectMonth: (year: Int, month: Int) -> Unit,
    onSelectYear: (year: Int) -> Unit,
    onSelectRange: (startDate: LocalDate, endDate: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    initialCalendarExpanded: Boolean = false,
    initialSearchActive: Boolean = false,
    cardAppearance: LedgerCardAppearance = LedgerCardAppearance(),
    cardBackground: ImageBitmap? = null,
    cardSettingsEnabled: Boolean = true,
    onUploadCardBackground: () -> Unit = {},
    onCardTransparencyEnabledChange: (Boolean) -> Unit = {},
    onCardTransparencyChange: (Int) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var isSearchActive by rememberSaveable { mutableStateOf(initialSearchActive) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var submittedSearchQuery by rememberSaveable { mutableStateOf("") }
    var isDateDropdownOpen by rememberSaveable { mutableStateOf(initialCalendarExpanded) }
    var calendarSession by remember { mutableIntStateOf(0) }
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
                val confirmedSelection = remember(uiState.filterStartDate, uiState.filterEndDate) {
                    val start = uiState.filterStartDate
                    val end = uiState.filterEndDate
                    if (start != null && end != null && !end.isBefore(start)) {
                        CalendarSelection(start, end)
                    } else {
                        CalendarSelection.week(LocalDate.now())
                    }
                }
                val calendarState = remember(calendarSession) {
                    CalendarPickerState(YearMonth.of(uiState.year, uiState.month), confirmedSelection)
                }
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
                    // 原型同款顶栏 + 自然流畅舒展的搜索框动效 + 月日历折叠视图 + 月度结余大卡片 (卡片与顶栏间距减半至 5.dp)
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
                                            CalendarRangeSelector(
                                                selection = if (isDateDropdownOpen) calendarState.selection else confirmedSelection,
                                                expanded = isDateDropdownOpen,
                                                onClick = {
                                                    if (!isDateDropdownOpen) calendarSession += 1
                                                    isDateDropdownOpen = !isDateDropdownOpen
                                                },
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
                                                imageVector = Icons.Default.Search,
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
                                            imageVector = Icons.Default.Search,
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
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "关闭搜索",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(24.dp),
                                            )
                                        }
                                    }
                                }
                            }

                            AnimatedVisibility(
                                visible = isDateDropdownOpen,
                                enter = fadeIn(tween(250)) + expandVertically(tween(300)),
                                exit = fadeOut(tween(200)) + shrinkVertically(tween(250)),
                            ) {
                                key(calendarSession) {
                                    MonthCalendarDatePickerCard(
                                        state = calendarState,
                                        onDismiss = { isDateDropdownOpen = false },
                                        onConfirm = { startDate, endDate ->
                                            if (ChronoUnit.DAYS.between(startDate, endDate) == 6L && startDate.dayOfWeek == DayOfWeek.MONDAY) {
                                                onSelectWeek(startDate)
                                            } else if (startDate.dayOfMonth == 1 && endDate == YearMonth.from(startDate).atEndOfMonth()) {
                                                onSelectMonth(startDate.year, startDate.monthValue)
                                            } else if (startDate == LocalDate.of(startDate.year, 1, 1) && endDate == LocalDate.of(startDate.year, 12, 31)) {
                                                onSelectYear(startDate.year)
                                            } else {
                                                onSelectRange(startDate, endDate)
                                            }
                                            isDateDropdownOpen = false
                                        },
                                    )
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
                                onTransparencyEnabledChange = onCardTransparencyEnabledChange,
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
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = dayGroup.displayDate,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (dayGroup.totalExpense > 0) {
                    Text(
                        text = "– ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosRed,
                    )
                    Text(
                        text = dayGroup.totalExpense.toFormattedAmount(showSymbol = true),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (dayGroup.totalExpense > 0 && dayGroup.totalIncome > 0) {
                    Text(
                        text = " / ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (dayGroup.totalIncome > 0) {
                    Text(
                        text = "+ ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = IosGreen,
                    )
                    Text(
                        text = dayGroup.totalIncome.toFormattedAmount(showSymbol = true),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (dayGroup.totalExpense == 0L && dayGroup.totalIncome == 0L) {
                    Text(
                        text = "¥0.00",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 2.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = Color.Black.copy(alpha = 0.04f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface),
        ) {
            dayGroup.transactions.forEachIndexed { index, detail ->
                DayTransactionRowItem(detail = detail)
                if (index < dayGroup.transactions.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 66.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
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
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val timeStr = remember(tx.transactionTime) {
        Instant.ofEpochMilli(tx.transactionTime).atZone(ZoneId.systemDefault()).format(timeFormatter)
    }

    val (iconBgColor, iconVector) = remember(detail.category.category.name, tx.type) {
        when {
            detail.category.category.name.contains("餐饮") -> Pair(IosOrange, Icons.Default.Restaurant)
            detail.category.category.name.contains("交通") -> Pair(IosBlue, Icons.Default.DirectionsSubway)
            detail.category.category.name.contains("购物") -> Pair(IosRed, Icons.Default.ShoppingBag)
            detail.category.category.name.contains("娱乐") -> Pair(IosPurple, Icons.Default.Coffee)
            tx.type == TransactionType.INCOME -> Pair(IosGreen, Icons.Default.Payments)
            else -> Pair(Color(0xFF5856D6), Icons.Default.Payments)
        }
    }

    val amountText = when (tx.type) {
        TransactionType.EXPENSE -> "– ${tx.amount.toFormattedAmount(showSymbol = true)}"
        TransactionType.INCOME -> "+ ${tx.amount.toFormattedAmount(showSymbol = true)}"
        TransactionType.TRANSFER -> tx.amount.toFormattedAmount(showSymbol = true)
    }
    val amountColor = when (tx.type) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
        TransactionType.INCOME -> IosGreen
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.onSurface
    }

    // 1:1 复刻设计图 (40dp 圆角图标 + 分类名称1号字 + 账户独立字色无背景框 + 备注2号字 + 金额1号字 + 时间2号字)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 左侧分类图标 (40dp x 40dp, 12dp 圆角，纯白 20dp 图标)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = detail.category.category.name,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }

            // 中间两行文字信息 (第1行分类名称 1号字; 第2行账户独立字色+备注 2号字)
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // 分类名称：1号字
                Text(
                    text = detail.category.category.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.2).sp,
                )
                // 账户 (单纯改变颜色为 IosBlue，无背景框) 与 备注 (2号字)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = detail.account.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = IosBlue,
                    )

                    if (!tx.note.isNullOrBlank()) {
                        Text(
                            text = tx.note,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        // 右侧两行金额与时间 (第1行具体支出/收入 1号字; 第2行时间 2号字)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = amountText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = amountColor,
                letterSpacing = (-0.3).sp,
            )
            Text(
                text = timeStr,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            onSelectWeek = {},
            onSelectMonth = { _, _ -> },
            onSelectYear = {},
            onSelectRange = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LedgerContentCalendarExpandedPreview() {
    MaterialTheme {
        LedgerContent(
            initialCalendarExpanded = true,
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
                        totalExpense = 18600L,
                        totalIncome = 0L,
                        transactions = listOf(
                            TransactionDetail(
                                transaction = Transaction(
                                    id = 1L,
                                    type = TransactionType.EXPENSE,
                                    amount = 14200L,
                                    accountId = 1L,
                                    categoryId = 1L,
                                    transactionTime = System.currentTimeMillis(),
                                    note = "盒马鲜生 · 周末采买",
                                ),
                                account = Account(id = 1L, name = "支付宝", type = AccountType.WALLET),
                                category = CategoryWithIcon(
                                    category = Category(id = 1L, name = "餐饮食品", type = CategoryType.EXPENSE, iconId = 1L),
                                    icon = ModelIcon(id = 1L, keyName = "ic_food", displayName = "餐饮", iconUri = "res://"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            onSelectWeek = {},
            onSelectMonth = { _, _ -> },
            onSelectYear = {},
            onSelectRange = { _, _ -> },
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
            onSelectWeek = {},
            onSelectMonth = { _, _ -> },
            onSelectYear = {},
            onSelectRange = { _, _ -> },
        )
    }
}
