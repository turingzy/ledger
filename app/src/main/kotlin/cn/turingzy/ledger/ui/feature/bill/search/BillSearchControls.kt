package cn.turingzy.ledger.ui.feature.bill.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.base.BottomSheetHeaderBase
import cn.turingzy.ledger.ui.component.input.FormTextField
import cn.turingzy.ledger.ui.feature.bill.sheet.RecordAccountSheet
import androidx.compose.ui.unit.Dp
import cn.turingzy.ledger.ui.component.base.IconButtonBase
import cn.turingzy.ledger.ui.component.base.ListRowBase
import cn.turingzy.ledger.ui.component.base.ModalBottomSheetBase
import cn.turingzy.ledger.ui.component.picker.LedgerDatePickerDialog
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.util.AmountFormatter
import cn.turingzy.ledger.util.toFormattedAmount
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private enum class SearchPanel { TIME, AMOUNT, SORT, ACCOUNT }

/**
 * 账单搜索筛选控件栏。
 *
 * 以横向滚动胶囊按钮的形式展示“时间”、“金额”、“排序方式”以及“账户”快捷筛选选项，
 * 点击时打开对应的底部筛选抽屉，用户确认后更新搜索请求参数。
 *
 * @param request 当前搜索请求参数 [BillSearchRequest]。
 * @param accounts 可选账户列表。
 * @param currency 当前计算的主货币代码。
 * @param onChange 筛选条件变更回调。
 */
@Composable
internal fun BillSearchControls(
    request: BillSearchRequest,
    accounts: List<AccountWithTemplate>,
    currency: String,
    onChange: (BillSearchRequest) -> Unit,
) {
    var panel by rememberSaveable { mutableStateOf<SearchPanel?>(null) }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val accountName = accounts.firstOrNull { it.account.id == request.accountId }?.account?.name
    val accountLabel = stringResource(R.string.ui_account)
    val timeActive = request.filters.start != null || request.filters.end != null
    val amountActive = request.filters.minimum != null || request.filters.maximum != null
    val timeLabel = if (timeActive) stringResource(request.datePreset?.label ?: R.string.bill_search_custom)
        else stringResource(R.string.bill_search_time)
    val amountLabel = if (amountActive) stringResource(request.amountPreset?.label ?: R.string.bill_search_custom)
        else stringResource(R.string.ui_amount)
    fun open(next: SearchPanel) {
        focusManager.clearFocus()
        keyboard?.hide()
        panel = next
    }
    fun apply(next: BillSearchRequest) {
        onChange(next)
        panel = null
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag("billSearchControls"),
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
    ) {
        SearchPill(timeLabel, { open(SearchPanel.TIME) },
            active = timeActive, expanded = panel == SearchPanel.TIME)
        SearchPill(amountLabel, { open(SearchPanel.AMOUNT) },
            active = amountActive, expanded = panel == SearchPanel.AMOUNT)
        SearchPill(
            if (request.sort == BillSearchSort.DEFAULT) stringResource(R.string.bill_search_sort)
            else stringResource(request.sort.labelResource()),
            { open(SearchPanel.SORT) }, active = request.sort != BillSearchSort.DEFAULT,
            expanded = panel == SearchPanel.SORT,
        )
        SearchPill(accountName ?: accountLabel, { open(SearchPanel.ACCOUNT) },
            active = request.accountId != null, expanded = panel == SearchPanel.ACCOUNT)
    }
    val dismiss = { panel = null }
    when (panel) {
        SearchPanel.TIME -> SearchDateDrawer(request.filters, request.datePreset, dismiss) { filters, preset ->
            apply(request.copy(filters = filters, datePreset = preset))
        }
        SearchPanel.AMOUNT -> SearchAmountDrawer(request.filters, request.amountPreset, currency, dismiss) { filters, preset ->
            apply(request.copy(filters = filters, amountPreset = preset))
        }
        SearchPanel.SORT -> SearchChoiceDrawer(
            stringResource(R.string.bill_search_sort),
            BillSearchSort.entries.map { it to stringResource(it.drawerLabelResource()) },
            request.sort, dismiss,
        ) { apply(request.copy(sort = it)) }
        SearchPanel.ACCOUNT -> RecordAccountSheet(
            title = stringResource(R.string.ui_select_account),
            accounts = accounts,
            selectedId = request.accountId ?: 0L,
            transactionType = TransactionType.EXPENSE,
            height = LocalConfiguration.current.screenHeightDp.dp * 0.55f,
            onDismiss = dismiss,
            onSelect = { apply(request.copy(accountId = it)) },
            onSelectAll = { apply(request.copy(accountId = null)) },
            selectionColor = LedgerStyle.colors.info,
        )
        null -> Unit
    }
}

/**
 * 搜索筛选胶囊小按钮。
 *
 * @param label 显示的文本标签。
 * @param onClick 点击事件回调。
 * @param modifier 外部修饰符。
 * @param background 背景填充色。
 * @param active 是否处于激活/选中状态（激活时以主题高亮色显示文字）。
 * @param expanded 是否展开（为 true 时显示向下展开箭头，false 显示向右箭头，null 不显示图标）。
 */
@Composable
internal fun SearchPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = LedgerStyle.colors.fieldBackground,
    active: Boolean = false,
    expanded: Boolean? = null,
) {
    val colors = LedgerStyle.colors
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val fill by animateColorAsState(
        if (pressed) lerp(background, colors.textPrimary, 0.13f) else background,
        animationSpec = tween(90), label = "SearchPillPress",
    )
    Row(
        modifier.heightIn(min = 32.dp).clip(LedgerStyle.shapes.pill).background(fill)
            .clickable(source, indication = null, role = Role.Button, onClick = rememberUiAction(onClick))
            .padding(horizontal = LedgerStyle.spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded == null) Arrangement.Center
        else Arrangement.spacedBy(3.dp, Alignment.Start),
    ) {
        val tint = if (active) colors.info else colors.textPrimary
        Text(
            text = label,
            style = LedgerStyle.typography.bodySmall,
            color = tint,
            maxLines = 1,
            overflow = if (expanded == null) TextOverflow.Ellipsis else TextOverflow.Clip,
        )
        if (expanded != null) Icon(
            if (expanded) MaterialSymbols.ExpandMore else MaterialSymbols.ChevronRight,
            contentDescription = null, modifier = Modifier.size(LedgerStyle.icons.small), tint = tint,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchDrawer(
    title: String,
    onDismiss: () -> Unit,
    onApply: (() -> Unit)? = null,
    applyEnabled: Boolean = true,
    sheetHeight: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val maxContentHeight = LocalConfiguration.current.screenHeightDp.dp * 0.68f
    ModalBottomSheetBase(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        contentHandlesInsets = sheetHeight != null,
    ) {
        // A fixed height includes the title and system navigation inset.
        val containerSize = if (sheetHeight == null) Modifier else Modifier.height(sheetHeight).navigationBarsPadding()
        Column(Modifier.fillMaxWidth().then(containerSize)) {
            BottomSheetHeaderBase(
                stringResource(R.string.ui_cancel), onClose = {
                    scope.launch {
                        sheetState.hide()
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
                action = onApply?.let { apply ->
                    {
                        IconButtonBase(
                            MaterialSymbols.Check, stringResource(R.string.ui_confirm), apply,
                            enabled = applyEnabled, contentColor = LedgerStyle.colors.info, showPressFeedback = false,
                        )
                    }
                },
            ) { Text(title, style = LedgerStyle.typography.cardTitle) }
            val contentSize = if (sheetHeight == null) Modifier.heightIn(max = maxContentHeight) else Modifier.weight(1f)
            Column(Modifier.fillMaxWidth().then(contentSize).padding(horizontal = LedgerStyle.spacing.section),
                content = content)
        }
    }
}

@Composable
private fun <T> SearchChoiceDrawer(
    title: String,
    choices: List<Pair<T, String>>,
    selected: T,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit,
) {
    SearchDrawer(title, onDismiss) {
        LazyColumn(contentPadding = PaddingValues(
            top = LedgerStyle.spacing.medium,
            bottom = LedgerStyle.spacing.extraLarge,
        )) {
            items(choices) { (value, label) ->
                ListRowBase(
                    headline = { Text(label, style = LedgerStyle.typography.body) },
                    trailing = if (value == selected) ({
                        Icon(
                            MaterialSymbols.Check, stringResource(R.string.ui_selected),
                            modifier = Modifier.size(LedgerStyle.icons.inline), tint = LedgerStyle.colors.info,
                        )
                    }) else null,
                    contentPadding = PaddingValues(vertical = LedgerStyle.spacing.rowVertical),
                    onClick = { onSelect(value) },
                )
            }
        }
    }
}

private enum class DateBound { START, END }

@Composable
private fun SearchDateDrawer(
    filters: BillSearchFilters,
    selectedPreset: SearchDatePreset?,
    onDismiss: () -> Unit,
    onApply: (BillSearchFilters, SearchDatePreset?) -> Unit,
) {
    val today = LocalDate.now()
    var start by rememberSaveable { mutableStateOf(filters.start) }
    var end by rememberSaveable { mutableStateOf(filters.end) }
    var bound by rememberSaveable { mutableStateOf<DateBound?>(null) }
    val draft = filters.copy(start = start, end = end)
    val format = remember { DateTimeFormatter.ofPattern("yyyy/MM/dd") }
    val valid = draft.start == null || draft.end == null || draft.start <= draft.end
    SearchDrawer(
        stringResource(R.string.bill_search_date_range), onDismiss,
        onApply = { onApply(draft, if (draft.start == null && draft.end == null) SearchDatePreset.ALL else null) },
        applyEnabled = valid,
        sheetHeight = LocalConfiguration.current.screenHeightDp.dp / 3,
    ) {
        val pillBackground = LedgerStyle.colors.overlayFieldBackground
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = LedgerStyle.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small)) {
                SearchDatePreset.entries.forEach { preset ->
                    val updated = preset.applyTo(filters, today)
                    SearchPill(stringResource(preset.label), { onApply(updated, preset) },
                        background = pillBackground, active = selectedPreset == preset && draft == updated)
                }
            }
            Text(stringResource(R.string.bill_search_custom_time), style = LedgerStyle.typography.bodyMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchPill(start?.format(format) ?: stringResource(R.string.ui_start_date),
                    { bound = DateBound.START }, Modifier.weight(1f),
                    background = pillBackground, active = bound == DateBound.START)
                Text(stringResource(R.string.bill_search_to), style = LedgerStyle.typography.bodyMedium)
                SearchPill(end?.format(format) ?: stringResource(R.string.ui_end_date),
                    { bound = DateBound.END }, Modifier.weight(1f),
                    background = pillBackground, active = bound == DateBound.END)
            }
            if (!valid) Text(stringResource(R.string.bill_search_invalid_date_range), color = MaterialTheme.colorScheme.error)
        }
    }
    bound?.let { selection ->
        val selectedDate = if (selection == DateBound.START) start else end
        LedgerDatePickerDialog(
            initialDate = selectedDate ?: today,
            onDismiss = { bound = null },
            onConfirm = { date ->
                if (selection == DateBound.START) start = date else end = date
                bound = null
            },
        )
    }
}

@Composable
private fun SearchAmountDrawer(
    filters: BillSearchFilters,
    selectedPreset: SearchAmountPreset?,
    currency: String,
    onDismiss: () -> Unit,
    onApply: (BillSearchFilters, SearchAmountPreset?) -> Unit,
) {
    var minimum by rememberSaveable { mutableStateOf(filters.minimum?.toFormattedAmount().orEmpty()) }
    var maximum by rememberSaveable { mutableStateOf(filters.maximum?.toFormattedAmount().orEmpty()) }
    val minCents = AmountFormatter.parseToCents(minimum)
    val maxCents = AmountFormatter.parseToCents(maximum)
    val invalidMinimum = minimum.isNotBlank() && (minCents == null || minCents < 0)
    val invalidMaximum = maximum.isNotBlank() && (maxCents == null || maxCents < 0)
    val invalidOrder = !invalidMinimum && !invalidMaximum && minCents != null && maxCents != null && minCents > maxCents
    val valid = !invalidMinimum && !invalidMaximum && !invalidOrder
    SearchDrawer(
        stringResource(R.string.bill_search_amount_range, currency), onDismiss,
        {
            onApply(filters.copy(minimum = minCents, maximum = maxCents),
                if (minCents == null && maxCents == null) SearchAmountPreset.ALL else null)
        },
        applyEnabled = valid,
        sheetHeight = LocalConfiguration.current.screenHeightDp.dp / 3,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(vertical = LedgerStyle.spacing.extraLarge),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small)) {
                SearchAmountPreset.entries.forEach { preset ->
                    SearchPill(stringResource(preset.label), { onApply(preset.applyTo(filters), preset) },
                        background = LedgerStyle.colors.overlayFieldBackground,
                        active = selectedPreset == preset && valid && minCents == preset.minimum && maxCents == preset.maximum)
                }
            }
            Text(stringResource(R.string.bill_search_custom_amount), style = LedgerStyle.typography.bodyMedium)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FormTextField(stringResource(R.string.bill_search_minimum), minimum, { minimum = it },
                    Modifier.weight(1f), isError = invalidMinimum || invalidOrder, keyboardType = KeyboardType.Decimal)
                Text(stringResource(R.string.bill_search_to), style = LedgerStyle.typography.bodyMedium)
                FormTextField(stringResource(R.string.bill_search_maximum), maximum, { maximum = it },
                    Modifier.weight(1f), isError = invalidMaximum || invalidOrder, keyboardType = KeyboardType.Decimal)
            }
            if (!valid) Text(stringResource(R.string.bill_search_invalid_amount_range), color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun BillSearchSort.labelResource(): Int = when (this) {
    BillSearchSort.DEFAULT -> R.string.bill_search_default_sort
    BillSearchSort.AMOUNT_ASC -> R.string.bill_search_amount_asc
    BillSearchSort.AMOUNT_DESC -> R.string.bill_search_amount_desc
    BillSearchSort.TIME_ASC -> R.string.bill_search_time_asc
    BillSearchSort.TIME_DESC -> R.string.bill_search_time_desc
}


private fun BillSearchSort.drawerLabelResource(): Int = when (this) {
    BillSearchSort.DEFAULT -> R.string.bill_search_default_sort
    BillSearchSort.AMOUNT_ASC -> R.string.bill_search_by_amount_asc
    BillSearchSort.AMOUNT_DESC -> R.string.bill_search_by_amount_desc
    BillSearchSort.TIME_ASC -> R.string.bill_search_by_time_asc
    BillSearchSort.TIME_DESC -> R.string.bill_search_by_time_desc
}
