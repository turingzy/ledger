package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.ui.feature.bill.component.SummaryHeroBanner
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsScreen
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsViewModel
import cn.turingzy.ledger.ui.feature.bill.sheet.CreateTransactionSheet
import cn.turingzy.ledger.ui.component.base.ScrollTitleContainer
import androidx.compose.foundation.lazy.rememberLazyListState
import cn.turingzy.ledger.ui.component.picker.PeriodPickerSheet
import cn.turingzy.ledger.model.transaction.DateFilterType
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import cn.turingzy.ledger.ui.component.base.BasePageFrame
import cn.turingzy.ledger.ui.component.base.BaseCard
import cn.turingzy.ledger.ui.component.base.BaseDivider
import cn.turingzy.ledger.ui.component.base.BaseEmptyState
import cn.turingzy.ledger.ui.component.biz.BusinessTransactionRow
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.theme.LedgerStyle
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.ui.component.card.summary.TopBarIconButton
import cn.turingzy.ledger.ui.component.card.summary.CardBackgroundDialog
import cn.turingzy.ledger.ui.component.biz.categoryGlyph
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.util.toFormattedAmount
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScreen(
    showRecordSheet: Boolean,
    onRecordSheetDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillViewModel = hiltViewModel(),
    onNavigateToRecord: () -> Unit = {},
    onSelectionVisibleChange: (Boolean) -> Unit = {},
) {
    val transactionViewModel: TransactionDetailsViewModel = hiltViewModel()
    val cardViewModel: BillCardViewModel = hiltViewModel()
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

    val scope = rememberCoroutineScope()

    LaunchedEffect(showRecordSheet) {
        if (showRecordSheet) {
            transactionViewModel.selectTransaction(null)
        }
    }

    BillTransactionActions(
        uiState = uiState,
        viewModel = viewModel,
        transactionViewModel = transactionViewModel,
        snackbarHostState = snackbarHostState,
        onSelectionVisibleChange = onSelectionVisibleChange,
    ) { transactionRow ->
        BillContent(
            uiState = uiState,
            onSearch = viewModel::search,
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
            onTransactionClick = transactionViewModel::selectTransaction,
            onSelectPeriod = viewModel::selectPeriod,
            transactionRow = transactionRow,
        )
    }

    if (!showRecordSheet) TransactionDetailsScreen(
        onLoadFailure = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
        viewModel = transactionViewModel,
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
        CreateTransactionSheet(
            date = LocalDate.now(),
            onDismiss = onRecordSheetDismiss,
            viewModel = viewModel,
            transactionViewModel = transactionViewModel,
        )
    }
}

@Composable
fun BillContent(
    uiState: BillUiState,
    modifier: Modifier = Modifier,
    initialSearchActive: Boolean = false,
    onSearch: (String) -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onSelectPeriod: (DateFilterType, LocalDate) -> Unit = { _, _ -> },
    cardAppearance: LedgerCardAppearance = LedgerCardAppearance(),
    cardBackground: ImageBitmap? = null,
    cardSettingsEnabled: Boolean = true,
    onUploadCardBackground: () -> Unit = {},
    onCardTransparencyChange: (Int) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    transactionRow: (@Composable (TransactionDetail) -> Unit)? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var isSearchActive by rememberSaveable { mutableStateOf(initialSearchActive) }
    var showPeriodPicker by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var submittedSearchQuery by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(submittedSearchQuery) { onSearch(submittedSearchQuery) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            delay(240)
            focusRequester.requestFocus()
        }
    }

    BasePageFrame(modifier) {
        when (uiState) {
            BillUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onSurface)
                }
            }

            is BillUiState.Success -> {
                val filteredDayGroups = uiState.dayGroups

                val listState = rememberLazyListState()
                ScrollTitleContainer(
                    title = textResources.getString(cn.turingzy.ledger.R.string.ui_bill),
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
                                                text = textResources.getString(cn.turingzy.ledger.R.string.ui_bill),
                                                fontSize = LedgerStyle.typography.screenTitle.fontSize,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                letterSpacing = (-0.6).sp,
                                                modifier = Modifier.align(Alignment.CenterStart),
                                            )
                                            Row(
                                                modifier = Modifier.align(Alignment.Center)
                                                    .clip(LedgerStyle.shapes.pill)
                                                    .background(LedgerStyle.colors.surface)
                                                    .semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_select_ledger_period) }
                                                    .uiClickable(kind = UiInteractionKind.Navigation) { showPeriodPicker = true }
                                                    .padding(horizontal = 12.dp, vertical = 3.dp),
                                                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Text(
                                                    text = uiState.monthDisplay,
                                                    fontSize = LedgerStyle.typography.sectionTitle.fontSize,
                                                    fontWeight = FontWeight.Medium,
                                                    color = LedgerStyle.colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Icon(MaterialSymbols.DateRange, null, Modifier.size(17.dp), tint = LedgerStyle.colors.textPrimary)
                                            }

                                            TopBarIconButton(
                                                imageVector = MaterialSymbols.Search,
                                                contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_search),
                                                onClick = rememberUiAction({ isSearchActive = true }, UiInteractionKind.Navigation),
                                                modifier = Modifier.align(Alignment.CenterEnd),
                                            )
                                        }
                                    }

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
                                                .clip(LedgerStyle.shapes.pill)
                                                .background(LedgerStyle.colors.fieldBackground)
                                                .padding(horizontal = LedgerStyle.spacing.large),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
                                        ) {
                                            Icon(
                                                imageVector = MaterialSymbols.Search,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(LedgerStyle.icons.default),
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
                                                    textStyle = LedgerStyle.typography.searchInput.copy(color = LedgerStyle.colors.textPrimary),
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
                                                                    text = textResources.getString(cn.turingzy.ledger.R.string.ui_search_notes_categories_accounts),
                                                                    fontSize = LedgerStyle.typography.transactionTitle.fontSize,
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

                                            TopBarIconButton(
                                                imageVector = MaterialSymbols.Close,
                                                contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_close_search),
                                                onClick = rememberUiAction({
                                                    isSearchActive = false
                                                    searchQuery = ""
                                                    submittedSearchQuery = ""
                                                    focusManager.clearFocus()
                                                }, UiInteractionKind.Navigation),
                                            )
                                        }
                                    }
                                }

                                if (uiState.amountsAvailable) SummaryHeroBanner(
                                    cardMonthDisplay = uiState.cardMonthDisplay,
                                    balance = uiState.monthBalance,
                                    expense = uiState.monthExpense,
                                    income = uiState.monthIncome,
                                    currency = uiState.currency,
                                    appearance = cardAppearance,
                                    background = cardBackground,
                                    settingsEnabled = cardSettingsEnabled,
                                    onUploadBackground = onUploadCardBackground,
                                    onTransparencyChange = onCardTransparencyChange,
                                ) else Text(textResources.getString(cn.turingzy.ledger.R.string.ui_exchange_rate_needed_refresh_or_enter_a_rate), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = LedgerStyle.typography.bodySmall.fontSize)
                            }
                        }

                        if (filteredDayGroups.isEmpty()) {
                            item(key = "empty_placeholder") {
                                BaseEmptyState(
                                    title = if (submittedSearchQuery.isNotBlank()) textResources.getString(cn.turingzy.ledger.R.string.ui_no_matching_transactions)
                                        else textResources.getString(cn.turingzy.ledger.R.string.ui_no_transactions_for_this_period_tap_to_add),
                                    contentPadding = PaddingValues(vertical = LedgerStyle.spacing.block * 2),
                                    titleStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = LedgerStyle.typography.bodyMedium.fontSize),
                                    titleColor = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        } else {
                            filteredDayGroups.forEach { dayGroup ->
                                item(key = "group_${dayGroup.dateEpochDay}") {
                                    DayGroupSection(dayGroup = dayGroup, onTransactionClick = onTransactionClick, currency = uiState.currency, transactionRow = transactionRow)
                                }
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
    val periodState = uiState as? BillUiState.Success
    if (showPeriodPicker && periodState != null) {
        PeriodPickerSheet(
            type = periodState.filterType,
            initialDate = periodState.filterStartDate ?: LocalDate.of(periodState.year, periodState.month, 1),
            onDismiss = { showPeriodPicker = false },
            onSelect = onSelectPeriod,
        )
    }

}

@Composable
fun DayGroupSection(
    dayGroup: DayTransactionGroup,
    modifier: Modifier = Modifier,
    onTransactionClick: (Long) -> Unit = {},
    currency: String = "CNY",
    transactionRow: (@Composable (TransactionDetail) -> Unit)? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    var expanded by rememberSaveable(dayGroup.dateEpochDay) { mutableStateOf(true) }
    val summary = buildList {
        if (dayGroup.transactions.any { it.transaction.type != TransactionType.TRANSFER && it.primaryAmount(currency) == null }) return@buildList
        if (dayGroup.totalExpense != 0L) add(textResources.getString(cn.turingzy.ledger.R.string.ui_expense) + ": ${dayGroup.totalExpense.toFormattedAmount()}")
        if (dayGroup.totalIncome != 0L) add(textResources.getString(cn.turingzy.ledger.R.string.ui_income) + ": ${dayGroup.totalIncome.toFormattedAmount()}")
    }.joinToString("  ")
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.tiny)) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .semantics { stateDescription = if (expanded) textResources.getString(cn.turingzy.ledger.R.string.ui_expanded) else textResources.getString(cn.turingzy.ledger.R.string.ui_collapsed) }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { expanded = !expanded }
                .padding(start = 4.dp, top = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = java.time.LocalDate.ofEpochDay(dayGroup.dateEpochDay).format(DateTimeFormatter.ofPattern("MM/dd E", locale)),
                modifier = Modifier.padding(end = 4.dp),
                maxLines = 1,
                fontSize = LedgerStyle.typography.bodySmall.fontSize,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = colors.onSurfaceVariant,
            )
            if (summary.isEmpty()) Spacer(Modifier.weight(1f))
            else Text(
                text = summary,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                fontSize = LedgerStyle.typography.bodySmall.fontSize,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
                color = colors.onSurfaceVariant,
            )
            Icon(
                if (expanded) MaterialSymbols.ExpandMore else MaterialSymbols.ChevronRight,
                contentDescription = if (expanded) textResources.getString(cn.turingzy.ledger.R.string.ui_hide_transactions) else textResources.getString(cn.turingzy.ledger.R.string.ui_show_transactions),
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(LedgerStyle.icons.default),
            )
        }
        if (expanded) {
            BaseCard(
                shape = LedgerStyle.shapes.summaryCard,
                contentPadding = PaddingValues(LedgerStyle.spacing.none),
                contentSpacing = LedgerStyle.spacing.none,
            ) {
                dayGroup.transactions.forEachIndexed { index, detail ->
                    key(detail.transaction.id) {
                        if (index > 0) BaseDivider(Modifier.padding(start = 62.dp, end = 14.dp))
                        if (transactionRow != null) transactionRow(detail)
                        else DayTransactionRowItem(detail = detail, onClick = { onTransactionClick(detail.transaction.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun DayTransactionRowItem(
    detail: TransactionDetail,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = {},
) {
    val tx = detail.transaction
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val timeStr = remember(tx.transactionTime) {
        Instant.ofEpochMilli(tx.transactionTime).atZone(ZoneId.systemDefault()).format(timeFormatter)
    }
    BusinessTransactionRow(
        categoryName = detail.category.category.name,
        supportingText = listOfNotNull(detail.account.name, tx.note?.takeIf { it.isNotBlank() }).joinToString(" · "),
        timeText = timeStr,
        amount = tx.amount,
        currency = detail.account.currency,
        type = tx.type,
        icon = detail.category.icon.categoryGlyph(),
        modifier = modifier,
        onClick = onClick,
    )
}
