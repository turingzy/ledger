package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.ui.feature.bill.component.SummaryHeroBanner
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsScreen
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsViewModel
import cn.turingzy.ledger.ui.feature.bill.sheet.RecordCreateSheet
import cn.turingzy.ledger.ui.component.base.ScrollTitleContainerBase
import androidx.compose.foundation.lazy.rememberLazyListState
import cn.turingzy.ledger.ui.component.picker.PeriodPickerSheet
import cn.turingzy.ledger.ui.component.picker.PeriodPickerPill
import cn.turingzy.ledger.model.transaction.DateFilterType
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import cn.turingzy.ledger.ui.component.base.PageFrameBase
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.component.base.EmptyStateBase
import cn.turingzy.ledger.ui.component.biz.TransactionRowBiz
import cn.turingzy.ledger.ui.component.biz.currencySymbolBiz
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.component.biz.TransactionRowDividerBiz
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.ui.component.card.summary.TopBarIconButton
import cn.turingzy.ledger.ui.component.card.summary.rememberSummaryBackgroundPicker
import cn.turingzy.ledger.ui.component.biz.categoryGlyphBiz
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.util.toFormattedAmount
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 账单功能主页面入口组件。
 *
 * 整合账单月度/周期统计汇总卡片、按天分组的账单明细列表、交易滑动与批量操作、
 * 交易详情抽屉以及新建记账弹窗等。
 *
 * @param showRecordSheet 是否显示记账底部抽屉。
 * @param onRecordSheetDismiss 关闭记账抽屉的回调。
 * @param modifier 外部修饰符。
 * @param viewModel 账单业务逻辑 ViewModel。
 * @param onNavigateToRecord 跳转到记账的回调。
 * @param onSelectionVisibleChange 批量选择条可见性变化回调。
 * @param onOpenSearch 打开搜索页面的回调。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillScreen(
    showRecordSheet: Boolean,
    onRecordSheetDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BillViewModel = hiltViewModel(),
    onNavigateToRecord: () -> Unit = {},
    onSelectionVisibleChange: (Boolean) -> Unit = {},
    onOpenSearch: () -> Unit = {},
) {
    val transactionViewModel: TransactionDetailsViewModel = hiltViewModel()
    val cardViewModel: BillCardViewModel = hiltViewModel()
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

    val scope = rememberCoroutineScope()

    LaunchedEffect(showRecordSheet) {
        if (showRecordSheet) {
            transactionViewModel.selectTransaction(null)
        }
    }

    BillTransactionActions(
        dayGroups = (uiState as? BillUiState.Success)?.dayGroups.orEmpty(),
        viewModel = viewModel,
        transactionViewModel = transactionViewModel,
        snackbarHostState = snackbarHostState,
        onSelectionVisibleChange = onSelectionVisibleChange,
    ) { transactionRow ->
        BillContent(
            uiState = uiState,
            onOpenSearch = onOpenSearch,
            modifier = modifier,
            cardAppearance = cardUiState.appearance,
            cardBackground = background,
            cardSettingsEnabled = !cardUiState.loading && !cardUiState.importing,
            onUploadCardBackground = pickBackground,
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

    if (showRecordSheet) {
        RecordCreateSheet(
            date = LocalDate.now(),
            onDismiss = onRecordSheetDismiss,
            viewModel = viewModel,
            transactionViewModel = transactionViewModel,
        )
    }
}

/**
 * 账单列表内容区视图。
 *
 * 包含滚动标题容器、顶部周期选择药丸与搜索入口、汇总卡片以及按日分组的交易列表。
 *
 * @param uiState 当前账单界面状态 [BillUiState]。
 * @param modifier 外部修饰符。
 * @param onOpenSearch 打开搜索页面的回调。
 * @param onTransactionClick 点击单条交易的回调（传入交易 ID）。
 * @param onSelectPeriod 变更统计周期筛选的回调。
 * @param cardAppearance 汇总卡片的外观配置。
 * @param cardBackground 汇总卡片自定义背景图。
 * @param cardSettingsEnabled 是否允许修改卡片设置。
 * @param onUploadCardBackground 上传/更换卡片背景图的回调。
 * @param onCardTransparencyChange 调整卡片透明度百分比的回调。
 * @param snackbarHostState 用于展示轻量提示的 [SnackbarHostState]。
 * @param transactionRow 自定义交易行项渲染插槽，通常用于注入带滑动操作或多选状态的行项。
 */
@Composable
fun BillContent(
    uiState: BillUiState,
    modifier: Modifier = Modifier,
    onOpenSearch: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onSelectPeriod: (DateFilterType, LocalDate) -> Unit = { _, _ -> },
    cardAppearance: LedgerCardAppearance = LedgerCardAppearance(),
    cardBackground: ImageBitmap? = null,
    cardSettingsEnabled: Boolean = true,
    onUploadCardBackground: () -> Unit = {},
    onCardTransparencyChange: (Int) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    transactionRow: (@Composable (detail: TransactionDetail, roundTop: Boolean, roundBottom: Boolean) -> Unit)? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var showPeriodPicker by rememberSaveable { mutableStateOf(false) }

    PageFrameBase(modifier) {
        when (uiState) {
            BillUiState.Loading -> Unit
            is BillUiState.Success -> {
                val dayGroups = uiState.dayGroups

                val listState = rememberLazyListState()
                ScrollTitleContainerBase(
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
                            start = LedgerStyle.spacing.pageHorizontal, end = LedgerStyle.spacing.pageHorizontal,
                            top = LedgerStyle.spacing.extraSmall,
                            bottom = LedgerStyle.spacing.block + LocalBottomBarContentPadding.current,
                        ),
                        verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.sectionGap),
                    ) {
                        item(key = "header_and_hero") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.labelGap),
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(44.dp),
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
                                    PeriodPickerPill(
                                        text = uiState.monthDisplay,
                                        onClick = { showPeriodPicker = true },
                                        modifier = Modifier.align(Alignment.Center),
                                        description = textResources.getString(cn.turingzy.ledger.R.string.ui_select_ledger_period),
                                        trailingIcon = MaterialSymbols.DateRange,
                                    )

                                    TopBarIconButton(
                                        imageVector = MaterialSymbols.Search,
                                        contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_search),
                                        onClick = rememberUiAction(onOpenSearch, UiInteractionKind.Navigation),
                                        modifier = Modifier.align(Alignment.CenterEnd),
                                    )
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

                        if (dayGroups.isEmpty()) {
                            item(key = "empty_placeholder") {
                                EmptyStateBase(
                                    title = textResources.getString(cn.turingzy.ledger.R.string.ui_no_transactions_for_this_period_tap_to_add),
                                    contentPadding = PaddingValues(vertical = LedgerStyle.spacing.block * 2),
                                    titleStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = LedgerStyle.typography.bodyMedium.fontSize),
                                    titleColor = LedgerStyle.colors.textSupporting,
                                )
                            }
                        } else {
                            dayGroups.forEach { dayGroup ->
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

/**
 * 单日账单分组组件。
 *
 * 渲染某一日期的分组标题（日期和该日主币种收支汇总）及该日对应的交易记录卡片列表。
 *
 * @param dayGroup 当前日期的分组数据对象 [DayTransactionGroup]。
 * @param modifier 外部修饰符。
 * @param onTransactionClick 点击单条交易的回调（传入交易 ID）。
 * @param currency 用于展示每日收支汇总的主货币代码。
 * @param transactionRow 可选的自定义交易行项渲染函数；为空时使用默认的 [DayTransactionRowItem]。
 */
@Composable
fun DayGroupSection(
    dayGroup: DayTransactionGroup,
    modifier: Modifier = Modifier,
    onTransactionClick: (Long) -> Unit = {},
    currency: String = "CNY",
    transactionRow: (@Composable (detail: TransactionDetail, roundTop: Boolean, roundBottom: Boolean) -> Unit)? = null,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val currencySymbol = currencySymbolBiz(currency)
    val summary = buildList {
        if (dayGroup.transactions.any { it.transaction.type != TransactionType.TRANSFER && it.primaryAmount(currency) == null }) return@buildList
        if (dayGroup.totalExpense != 0L) {
            val amount = dayGroup.totalExpense.toFormattedAmount(showSymbol = true, symbol = currencySymbol)
            add(textResources.getString(cn.turingzy.ledger.R.string.ui_expense) + ": $amount")
        }
        if (dayGroup.totalIncome != 0L) {
            val amount = dayGroup.totalIncome.toFormattedAmount(showSymbol = true, symbol = currencySymbol)
            add(textResources.getString(cn.turingzy.ledger.R.string.ui_income) + ": $amount")
        }
    }.joinToString("  ")
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.tiny)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 3.dp, end = 4.dp),
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
        }
        val rows: @Composable () -> Unit = {
            dayGroup.transactions.forEachIndexed { index, detail ->
                key(detail.transaction.id) {
                    if (index > 0) TransactionRowDividerBiz()
                    if (transactionRow != null) transactionRow(detail, index == 0, index == dayGroup.transactions.lastIndex)
                    else DayTransactionRowItem(detail = detail, onClick = { onTransactionClick(detail.transaction.id) })
                }
            }
        }
        val shape = LedgerStyle.shapes.summaryCard
        if (transactionRow != null) {
            Column(Modifier.fillMaxWidth().background(colors.surface, shape)) { rows() }
        } else {
            CardBase(
                shape = shape,
                contentPadding = PaddingValues(LedgerStyle.spacing.none),
                contentSpacing = LedgerStyle.spacing.none,
            ) { rows() }
        }
    }
}

/**
 * 单条交易记录行组件。
 *
 * 展示交易分类图标与名称、账户名称、备注、格式化时间以及交易金额与正负符号。
 *
 * @param detail 交易详情数据 [TransactionDetail]。
 * @param modifier 外部修饰符。
 * @param onClick 点击事件回调。
 * @param showDate 时间显示是否包含月份和日期（例如跨日搜索结果中展示）。
 * @param timeText 调用方提供的时间文本，用于保留分析列表的日期格式。
 */
@Composable
fun DayTransactionRowItem(
    detail: TransactionDetail,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = {},
    showDate: Boolean = false,
    timeText: String? = null,
) {
    val tx = detail.transaction
    val timeFormatter = remember(showDate) { DateTimeFormatter.ofPattern(if (showDate) "MM/dd HH:mm" else "HH:mm") }
    val timeStr = timeText ?: remember(tx.transactionTime, timeFormatter) {
        Instant.ofEpochMilli(tx.transactionTime).atZone(ZoneId.systemDefault()).format(timeFormatter)
    }
    TransactionRowBiz(
        categoryName = detail.category.category.name,
        timeText = timeStr,
        note = tx.note,
        amount = tx.amount,
        currency = detail.account.currency,
        type = tx.type,
        icon = detail.category.icon.categoryGlyphBiz(),
        modifier = modifier,
        onClick = onClick,
    )
}
