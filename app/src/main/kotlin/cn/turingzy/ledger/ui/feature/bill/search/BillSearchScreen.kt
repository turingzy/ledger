package cn.turingzy.ledger.ui.feature.bill.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.EmptyStateBase
import cn.turingzy.ledger.ui.component.base.PageFrameBase
import cn.turingzy.ledger.ui.component.base.ScrollTitleContainerBase
import cn.turingzy.ledger.ui.component.biz.LocalCurrencyBiz
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import cn.turingzy.ledger.ui.feature.bill.BillTransactionActions
import cn.turingzy.ledger.ui.feature.bill.BillViewModel
import cn.turingzy.ledger.ui.feature.bill.DayGroupSection
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsScreen
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsViewModel
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.component.biz.TransactionRowDividerBiz
import cn.turingzy.ledger.ui.theme.LedgerStyle
import kotlinx.coroutines.launch

/**
 * 账单全局搜索主页面。
 *
 * 支持按关键词（备注、分类、账户名）搜索、基于近期高频分类的快捷推荐、
 * 高级条件过滤（时间、金额、账户与排序），并完整复用账单列表的滑动操作、批量操作与详情编辑能力。
 *
 * @param onBack 返回上级页面的回调。
 * @param viewModel 账单 ViewModel。
 * @param transactionViewModel 交易详情及编辑 ViewModel。
 */
@Composable
internal fun BillSearchScreen(
    onBack: () -> Unit,
    viewModel: BillViewModel = hiltViewModel(),
    transactionViewModel: TransactionDetailsViewModel = hiltViewModel(),
) {
    var request by rememberSaveable { mutableStateOf(BillSearchRequest()) }
    var input by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun closeSearch() {
        focusManager.clearFocus()
        keyboard?.hide()
        onBack()
    }

    val searching = request.hasSearchTarget
    val results = if (searching) key(request) {
        val stream = remember { viewModel.search(request) }
        stream.collectAsStateWithLifecycle(initialValue = null).value
    } else null
    val suggestions = if (!searching) viewModel.searchSuggestions.collectAsStateWithLifecycle(emptyList()).value
        else emptyList()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    var focusPending by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val currency = results?.currency ?: LocalCurrencyBiz.current

    CompositionLocalProvider(LocalBottomBarContentPadding provides 0.dp) {
        BillTransactionActions(
            dayGroups = results?.groups.orEmpty(),
            ungroupedRows = results?.ungrouped.orEmpty(),
            viewModel = viewModel,
            transactionViewModel = transactionViewModel,
            snackbarHostState = snackbar,
            onSelectionVisibleChange = {},
        ) { transactionRow ->
            PageFrameBase {
                ScrollTitleContainerBase(
                    title = input.trim(),
                    scrollOffset = {
                        if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset else Int.MAX_VALUE
                    },
                    onScrollToTop = { listState.animateScrollToItem(0) },
                ) { scrollModifier ->
                    LazyColumn(
                        modifier = scrollModifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            start = LedgerStyle.spacing.pageHorizontal, end = LedgerStyle.spacing.pageHorizontal,
                            bottom = LedgerStyle.spacing.block + LocalBottomBarContentPadding.current,
                        ),
                    ) {
                        item(key = "searchHeader") {
                            BillSearchHeader(
                                input = input,
                                onInputChange = { value ->
                                    input = value
                                    request = request.copy(query = "", categoryId = null)
                                },
                                onSubmit = {
                                    request = request.copy(query = input.trim(), categoryId = null)
                                    focusManager.clearFocus()
                                    keyboard?.hide()
                                },
                                onClose = ::closeSearch,
                                focusRequester = focusRequester.takeIf { focusPending },
                                onFocusRequested = { focusPending = false },
                            )
                        }
                        if (searching) {
                            item(key = "searchControls") {
                                BillSearchControls(request, accounts, currency, onChange = { request = it })
                            }
                            if (results != null) searchResults(results, transactionRow)
                        } else if (suggestions.isNotEmpty()) item(key = "searchSuggestions") {
                            Column(
                                Modifier.padding(top = LedgerStyle.spacing.extraLarge),
                                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
                            ) {
                                Text(stringResource(R.string.bill_search_suggestions), style = LedgerStyle.typography.bodyMedium)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
                                    verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
                                ) {
                                    suggestions.forEach { suggestion ->
                                        SearchPill(suggestion.category.name, onClick = {
                                            input = suggestion.category.name
                                            request = request.copy(query = "", categoryId = suggestion.category.id)
                                            focusManager.clearFocus()
                                            keyboard?.hide()
                                        }, background = LedgerStyle.colors.surface)
                                    }
                                }
                            }
                        }
                    }
                }
                SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter)
                    .padding(bottom = LocalBottomBarContentPadding.current))
            }
        }
    }
    TransactionDetailsScreen(
        onLoadFailure = { message -> scope.launch { snackbar.showSnackbar(message) } },
        viewModel = transactionViewModel,
    )
}

/**
 * 搜索输入框头部栏组件。
 *
 * 封装胶囊样式搜索栏、搜索图标、输入文本框、键盘回车操作以及右侧关闭/返回按钮。
 *
 * @param input 当前输入的文本内容。
 * @param onInputChange 文本变更回调。
 * @param onSubmit 键盘按下搜索或回车时的提交回调。
 * @param onClose 点击关闭按钮的回调。
 * @param focusRequester 可选的焦点请求器，用于进入页面时自动弹出软键盘。
 * @param onFocusRequested 自动请求焦点完成的回调。
 */
@Composable
internal fun BillSearchHeader(
    input: String,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    focusRequester: FocusRequester? = null,
    onFocusRequested: () -> Unit = {},
) {
    val colors = LedgerStyle.colors
    val fieldShape = LedgerStyle.shapes.pill
    val searchLabel = stringResource(R.string.ui_search)
    Row(
        Modifier.fillMaxWidth().padding(vertical = LedgerStyle.spacing.medium)
            .heightIn(min = 44.dp).clip(fieldShape).background(colors.fieldBackground)
            .padding(start = LedgerStyle.spacing.large, end = LedgerStyle.spacing.extraSmall)
            .testTag("billSearchField"),
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val keyboard = LocalSoftwareKeyboardController.current
        LaunchedEffect(focusRequester) {
            if (focusRequester != null) {
                withFrameNanos {}
                focusRequester.requestFocus()
                keyboard?.show()
                onFocusRequested()
            }
        }
        Icon(MaterialSymbols.Search, null, Modifier.size(LedgerStyle.icons.default), tint = colors.textSecondary)
        BasicTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f)
                .then(if (focusRequester == null) Modifier else Modifier.focusRequester(focusRequester))
                .semantics { contentDescription = searchLabel },
            textStyle = LedgerStyle.typography.searchInput.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.textPrimary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { field ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (input.isEmpty()) Text(
                        stringResource(R.string.ui_search_notes_categories_accounts),
                        style = LedgerStyle.typography.searchInput, color = colors.textSupporting, maxLines = 1,
                    )
                    field()
                }
            },
        )
        Box(
            Modifier.size(44.dp).clip(fieldShape)
                .uiClickable(kind = UiInteractionKind.Navigation, onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(MaterialSymbols.Close, stringResource(R.string.ui_close_search),
                Modifier.size(LedgerStyle.icons.default), tint = colors.textSecondary)
        }
    }
}

/** Adds grouped bill rows or globally amount-sorted rows to the search scroll surface. */
private fun LazyListScope.searchResults(
    results: BillSearchResults,
    transactionRow: @Composable (detail: TransactionDetail, roundTop: Boolean, roundBottom: Boolean) -> Unit,
) {
    if (results.groups.isEmpty() && results.ungrouped.isEmpty()) {
        item(key = "noSearchResults") {
            EmptyStateBase(
                title = stringResource(R.string.ui_no_matching_transactions),
                contentPadding = PaddingValues(vertical = LedgerStyle.spacing.block * 2),
            )
        }
        return
    }
    item(key = "resultsSpacing") { Spacer(Modifier.height(LedgerStyle.spacing.large)) }
    results.groups.forEach { group ->
        item(key = "group_${group.dateEpochDay}") {
            DayGroupSection(group, Modifier.padding(bottom = 10.dp), currency = results.currency, transactionRow = transactionRow)
        }
    }
    itemsIndexed(results.ungrouped, key = { _, detail -> detail.transaction.id }) { index, detail ->
        val colors = LedgerStyle.colors
        val radius = LedgerStyle.shapes.summaryCard.topStart
        val square = androidx.compose.foundation.shape.CornerSize(0.dp)
        val roundTop = index == 0
        val roundBottom = index == results.ungrouped.lastIndex
        val top = if (roundTop) radius else square
        val bottom = if (roundBottom) radius else square
        Column(Modifier.fillMaxWidth().background(colors.surface, RoundedCornerShape(top, top, bottom, bottom))) {
            if (index > 0) TransactionRowDividerBiz()
            transactionRow(detail, roundTop, roundBottom)
        }
    }
}
