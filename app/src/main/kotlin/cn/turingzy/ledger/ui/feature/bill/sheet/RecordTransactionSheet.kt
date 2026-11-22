package cn.turingzy.ledger.ui.feature.bill.sheet

import cn.turingzy.ledger.ui.component.picker.LedgerDateTimeDialog
import cn.turingzy.ledger.ui.component.base.IconButtonContentBase
import cn.turingzy.ledger.ui.component.base.DividerBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.component.base.ModalBottomSheetBase
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.input.SegmentedControl
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.card.summary.SummaryCardIconButton
import cn.turingzy.ledger.model.currency.convertCents
import cn.turingzy.ledger.model.currency.CurrencyAmountOverride
import cn.turingzy.ledger.ui.component.base.BaseAlertDialog
import cn.turingzy.ledger.ui.component.base.keepSheetAnchorDuringIme
import androidx.compose.ui.text.input.KeyboardType
import cn.turingzy.ledger.ui.component.biz.LocalCurrencyBiz
import cn.turingzy.ledger.ui.component.biz.amountTextBiz
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.biz.categoryGlyphBiz
import cn.turingzy.ledger.util.AmountFormatter
import cn.turingzy.ledger.util.toFormattedAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class RecordPanel { ENTRY, ACCOUNT, DESTINATION, DATE_TIME }

/**
 * 记账与交易编辑的核心底部抽屉组件。
 *
 * 完整承载单条交易的新增或编辑流程：
 * - 顶部支出/收入/转账类型切换
 * - 分类选择 5 列自适应网格（转账模式下为转出与转入账户卡片及互换）
 * - 账户与交易发生时间快捷修改
 * - 跨币种转账汇率动态拉取与自定义调整
 * - 外币交易主币种折算金额查看与手动覆盖
 * - 金额四则运算表达式输入、备注录入以及内置数字键盘
 *
 * @param sheetState 控制抽屉展开收起的 [SheetState]。
 * @param categories 可选分类列表。
 * @param accounts 可选账户列表。
 * @param onDismissRequest 关闭抽屉请求回调。
 * @param onConfirm 提交交易记录的回调，包含类型、金额、账户、转账信息、分类、备注、时间、折算金额及汇率覆盖。
 * @param modifier 外部修饰符。
 * @param initialTransaction 编辑模式下的初始交易数据，为空表示新增。
 * @param initialTransactionTime 新增模式下的初始交易时间戳。
 * @param title 抽屉标题文本。
 * @param saving 是否正在提交保存中。
 * @param error 错误提示文案。
 * @param sheetHeight 抽屉整体高度。
 * @param onLoadExchangeRate 跨币种汇率异步加载方法。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTransactionSheet(
    sheetState: SheetState,
    categories: List<CategoryWithIcon>,
    accounts: List<AccountWithTemplate>,
    onDismissRequest: () -> Unit,
    onConfirm: (
        type: TransactionType, amount: Long, accountId: Long, toAccountId: Long?,
        categoryId: Long, note: String?, transactionTime: Long, toAmount: Long?, amountOverride: CurrencyAmountOverride?,
    ) -> Unit,
    modifier: Modifier = Modifier,
    initialTransaction: Transaction? = null,
    initialTransactionTime: Long? = null,
    title: String = androidx.compose.ui.res.stringResource(cn.turingzy.ledger.R.string.ui_new_transaction),
    saving: Boolean = false,
    error: String? = null,
    sheetHeight: Dp = 520.dp,
    onLoadExchangeRate: suspend (String, String, Long) -> String? = { _, _, _ -> null },
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val fixedHeight = remember { sheetHeight }
    var expression by rememberSaveable(initialTransaction?.id) { mutableStateOf(initialTransaction?.amount?.toFormattedAmount().orEmpty()) }
    var note by rememberSaveable(initialTransaction?.id) { mutableStateOf(initialTransaction?.note.orEmpty()) }
    var type by rememberSaveable(initialTransaction?.id) { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var accountId by rememberSaveable(initialTransaction?.id) { mutableLongStateOf(initialTransaction?.accountId ?: 0L) }
    var destinationId by rememberSaveable(initialTransaction?.id) { mutableLongStateOf(initialTransaction?.toAccountId ?: 0L) }
    var categoryId by rememberSaveable(initialTransaction?.id) { mutableLongStateOf(initialTransaction?.categoryId ?: 0L) }
    var transactionTime by rememberSaveable(initialTransaction?.id, initialTransactionTime) {
        mutableLongStateOf(initialTransaction?.transactionTime ?: initialTransactionTime ?: System.currentTimeMillis())
    }
    var panel by rememberSaveable { mutableStateOf(RecordPanel.ENTRY) }
    val focusManager = LocalFocusManager.current
    val zone = ZoneId.systemDefault()
    val matchingCategories = remember(categories, type) { selectableRecordCategories(categories, type) }
    LaunchedEffect(matchingCategories, type) {
        if (matchingCategories.isNotEmpty() && matchingCategories.none { it.category.id == categoryId }) {
            categoryId = matchingCategories.firstOrNull()?.category?.id ?: 0L
        }
    }
    val hasCalculation = expression.drop(1).any { it in "+-×÷" }
    val evaluatedExpression = if (hasCalculation) evaluateMathExpression(expression) else expression
    val enteredAmount = AmountFormatter.parseToCents(evaluatedExpression)
    val canEvaluate = !saving && hasCalculation && enteredAmount != null
    val sourceCurrency = accounts.firstOrNull { it.account.id == accountId }?.account?.currency
    val targetCurrency = accounts.firstOrNull { it.account.id == destinationId }?.account?.currency
    val crossCurrency = type == TransactionType.TRANSFER && sourceCurrency != null && targetCurrency != null && sourceCurrency != targetCurrency
    var transferRate by rememberSaveable(accountId, destinationId, transactionTime) {
        mutableStateOf(initialTransaction?.takeIf { it.accountId == accountId && it.toAccountId == destinationId && it.transactionTime == transactionTime }?.transferRate.orEmpty())
    }
    var rateEdited by remember(accountId, destinationId, transactionTime) { mutableStateOf(false) }
    var rateError by remember(accountId, destinationId, transactionTime) { mutableStateOf<String?>(null) }
    LaunchedEffect(sourceCurrency, targetCurrency, transactionTime, crossCurrency) {
        if (!crossCurrency || transferRate.isNotBlank()) return@LaunchedEffect
        try {
            val quote = onLoadExchangeRate(checkNotNull(sourceCurrency), checkNotNull(targetCurrency), transactionTime)
            if (!rateEdited) transferRate = quote.orEmpty()
        } catch (error: kotlinx.coroutines.CancellationException) { throw error }
        catch (error: Exception) { timber.log.Timber.w(error, "Unable to load transfer rate"); rateError = textResources.getString(cn.turingzy.ledger.R.string.ui_enter_an_exchange_rate) }
    }
    val transaction = RecordFormInput(type, expression, accountId, destinationId, categoryId, transactionTime, note, transferRate.takeIf { crossCurrency })
        .toTransactionOrNull(accounts, categories)

    val primaryCurrency = LocalCurrencyBiz.current
    val needsConversion = sourceCurrency != null && sourceCurrency != primaryCurrency
    val historicalAmount = initialTransaction?.takeIf {
        needsConversion && it.accountId == accountId && it.transactionTime == transactionTime && it.amount == enteredAmount
    }?.let { original ->
        original.currencySnapshots.firstOrNull { it.currency == primaryCurrency }?.amount
            ?: original.baseAmount.takeIf { original.baseCurrency == primaryCurrency }
    }
    var primaryRate by remember(sourceCurrency, primaryCurrency, transactionTime) { mutableStateOf<String?>(null) }
    LaunchedEffect(sourceCurrency, primaryCurrency, transactionTime, historicalAmount != null) {
        if (sourceCurrency == null || !needsConversion || historicalAmount != null || primaryRate != null) return@LaunchedEffect
        try {
            primaryRate = onLoadExchangeRate(sourceCurrency, primaryCurrency, transactionTime)
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: Exception) {
            timber.log.Timber.w(error, "Unable to load the primary currency rate")
        }
    }
    val canEditPrimaryAmount = needsConversion && enteredAmount != null && enteredAmount > 0L
    var manualAmount by rememberSaveable(accountId, primaryCurrency, transactionTime, enteredAmount) { mutableStateOf<Long?>(null) }
    var showAmountEditor by rememberSaveable(accountId, primaryCurrency, transactionTime, enteredAmount) { mutableStateOf(false) }
    val amountOverride = manualAmount?.takeIf { canEditPrimaryAmount }?.let { CurrencyAmountOverride(primaryCurrency, it) }
    val primaryAmount = amountOverride?.amount ?: historicalAmount ?: enteredAmount?.takeIf { it > 0L }?.let { amount ->
        primaryRate?.let { rate -> runCatching { convertCents(amount, rate) }.getOrNull() }
    }

    fun openPanel(target: RecordPanel) {
        if (saving) return
        focusManager.clearFocus()
        panel = target
    }

    ModalBottomSheetBase(
        onDismissRequest = onDismissRequest, sheetState = sheetState,
        modifier = modifier.keepSheetAnchorDuringIme(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        dragHandle = null,
        contentHandlesInsets = true,
    ) {
        RecordTransactionContent(
            modifier = Modifier.height(fixedHeight).testTag("recordDrawer")
                .semantics { paneTitle = title }.padding(WindowInsets.navigationBars.asPaddingValues()),
            expression = expression, note = note, type = type, categories = matchingCategories,
            selectedCategoryId = categoryId,
            currency = sourceCurrency ?: primaryCurrency,
            accountName = accounts.firstOrNull { it.account.id == accountId }?.account?.name,
            destinationName = accounts.firstOrNull { it.account.id == destinationId }?.account?.name,
            timeLabel = recordTimeLabel(transactionTime, zone),
            error = error ?: rateError,
            primaryAmountLabel = if (canEditPrimaryAmount) {
                val amountText = amountTextBiz(primaryAmount, primaryCurrency, primaryCurrency, useGrouping = true)
                when {
                    primaryAmount == null -> "$amountText $primaryCurrency"
                    amountOverride == null -> "≈ $amountText"
                    else -> amountText
                }
            } else null,
            onEditPrimaryAmount = { if (!saving) showAmountEditor = true },
            onDismiss = onDismissRequest,
            closeDescription = stringResource(R.string.ui_close_record_sheet, title),
            actionsEnabled = !saving,
            transferRate = transferRate.takeIf { crossCurrency },
            transferLabel = "1 ${sourceCurrency.orEmpty()} = … ${targetCurrency.orEmpty()}",
            transferredAmount = transaction?.toAmount?.toFormattedAmount()?.let { "$it ${targetCurrency.orEmpty()}" },
            onTransferRateChange = { rateEdited = true; transferRate = it; rateError = null },
            hasCalculation = hasCalculation,
            actionEnabled = if (hasCalculation) canEvaluate else !saving && transaction != null,
            onTypeChange = { selected ->
                if (!saving && selected != type) {
                    type = selected
                    categoryId = initialTransaction?.takeIf { it.type == selected }?.categoryId ?: 0L
                }
            },
            onCategorySelect = { if (!saving) categoryId = it },
            onExpressionChange = { if (!saving) expression = it },
            onNoteChange = { if (!saving) note = it },
            onAccountClick = { openPanel(RecordPanel.ACCOUNT) },
            onDestinationClick = { openPanel(RecordPanel.DESTINATION) },
            onTimeClick = { openPanel(RecordPanel.DATE_TIME) },
            onSwapAccounts = { if (!saving) { val previous = accountId; accountId = destinationId; destinationId = previous } },
            onConfirm = {
                if (!saving) {
                    if (hasCalculation) expression = evaluatedExpression
                    else transaction?.let { value ->
                        onConfirm(value.type, value.amount, value.accountId, value.toAccountId, value.categoryId, value.note, value.transactionTime, value.toAmount, amountOverride)
                    }
                }
            },
            keyHeight = if (fixedHeight < 540.dp) 40.dp else 48.dp,
        )
    }
    if (panel == RecordPanel.ACCOUNT || panel == RecordPanel.DESTINATION) {
        val destination = panel == RecordPanel.DESTINATION
        RecordAccountSheet(
            title = if (destination) textResources.getString(cn.turingzy.ledger.R.string.ui_select_destination_account) else if (type == TransactionType.TRANSFER) textResources.getString(cn.turingzy.ledger.R.string.ui_select_source_account) else textResources.getString(cn.turingzy.ledger.R.string.ui_select_account),
            accounts = accounts.filter { !it.account.isDeleted && (!destination || it.account.id != accountId) },
            selectedId = if (destination) destinationId else accountId,
            transactionType = type,
            height = fixedHeight * 0.65f,
            onDismiss = { panel = RecordPanel.ENTRY },
            onSelect = { id ->
                if (destination) destinationId = id else {
                    accountId = id
                    val selectedCurrency = accounts.firstOrNull { it.account.id == id }?.account?.currency
                    if (destinationId == id || accounts.firstOrNull { it.account.id == destinationId }?.account?.currency != selectedCurrency) destinationId = 0L
                }
                panel = RecordPanel.ENTRY
            },
        )
    }
    if (showAmountEditor && canEditPrimaryAmount) {
        PrimaryAmountEditor(
            currency = primaryCurrency,
            originalAmount = enteredAmount,
            initialAmount = primaryAmount,
            onDismiss = { showAmountEditor = false },
            onConfirm = {
                manualAmount = it
                showAmountEditor = false
            },
        )
    }
    if (panel == RecordPanel.DATE_TIME) {
        LedgerDateTimeDialog(
            transactionTime, onDismiss = { panel = RecordPanel.ENTRY },
            onConfirm = { transactionTime = it; panel = RecordPanel.ENTRY },
        )
    }
}

/**
 * 记账抽屉主体内容区布局。
 *
 * @param expression 当前金额表达式输入字符串。
 * @param note 交易备注。
 * @param type 交易类型（支出、收入、转账）。
 * @param categories 当前类型下匹配的分类列表。
 * @param selectedCategoryId 当前选中的分类 ID。
 * @param accountName 当前选中的账户名称。
 * @param destinationName 转账模式下的目标账户名称。
 * @param timeLabel 格式化后的时间文案。
 * @param hasCalculation 当前金额表达式是否包含待运算的四则运算符号。
 * @param actionEnabled 键盘操作按钮（完成或等号）是否可用。
 * @param onTypeChange 切换交易类型回调。
 * @param onCategorySelect 选中分类回调。
 * @param onExpressionChange 金额表达式变更回调。
 * @param onNoteChange 备注变更回调。
 * @param onAccountClick 点击选择账户回调。
 * @param onDestinationClick 点击选择转入账户回调。
 * @param onTimeClick 点击修改时间回调。
 * @param onSwapAccounts 转账模式下互换转出与转入账户回调。
 * @param onConfirm 点击完成或等号回调。
 * @param modifier 外部修饰符。
 * @param error 错误提示信息。
 * @param keyHeight 键盘按键高度。
 * @param currency 当前账户币种代码。
 * @param transferRate 跨币种转账汇率。
 * @param transferLabel 汇率说明标签。
 * @param transferredAmount 折算后的目标账户金额。
 * @param onTransferRateChange 修改转账汇率回调。
 * @param primaryAmountLabel 折算为主币种的金额文本。
 * @param onEditPrimaryAmount 点击编辑主币种折算金额回调。
 * @param onDismiss 关闭抽屉回调。
 * @param closeDescription 关闭按钮无障碍描述。
 * @param actionsEnabled 操作是否可用。
 */
@Composable
internal fun RecordTransactionContent(
    expression: String,
    note: String,
    type: TransactionType,
    categories: List<CategoryWithIcon>,
    selectedCategoryId: Long,
    accountName: String?,
    destinationName: String?,
    timeLabel: String,
    hasCalculation: Boolean,
    actionEnabled: Boolean,
    onTypeChange: (TransactionType) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onExpressionChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onAccountClick: () -> Unit,
    onDestinationClick: () -> Unit,
    onTimeClick: () -> Unit,
    onSwapAccounts: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyHeight: Dp = 48.dp,
    currency: String = "CNY",
    transferRate: String? = null,
    transferLabel: String = "",
    transferredAmount: String? = null,
    onTransferRateChange: (String) -> Unit = {},
    primaryAmountLabel: String? = null,
    onEditPrimaryAmount: () -> Unit = {},
    onDismiss: () -> Unit,
    closeDescription: String,
    actionsEnabled: Boolean = true,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val expressionScroll = rememberScrollState()
    var footerHeight by remember { mutableStateOf(0.dp) }
    val keyboardOverlap = with(density) {
        (WindowInsets.ime.getBottom(this) - WindowInsets.navigationBars.getBottom(this)).coerceAtLeast(0).toDp()
    }
    val keypadHeight = keyHeight * 4 + 24.dp
    val footerLift = (keyboardOverlap - keypadHeight - 8.dp).coerceAtLeast(0.dp)
    val selectionColor = LedgerStyle.colors.transaction(type)
    val amountColor = selectionColor
    LaunchedEffect(expression) { expressionScroll.animateScrollTo(expressionScroll.maxValue) }
    Column(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = LedgerStyle.spacing.large), contentAlignment = Alignment.Center) {
            SummaryCardIconButton(
                MaterialSymbols.Close, closeDescription, onDismiss,
                modifier = Modifier.align(Alignment.CenterStart), enabled = actionsEnabled,
                containerColor = colors.surface, contentColor = colors.onSurface, showPressFeedback = false,
            )
            SegmentedControl(
                listOf(textResources.getString(cn.turingzy.ledger.R.string.ui_expense), textResources.getString(cn.turingzy.ledger.R.string.ui_income), textResources.getString(cn.turingzy.ledger.R.string.ui_transfer)), TransactionType.entries.indexOf(type),
                { onTypeChange(TransactionType.entries[it]) },
                modifier = Modifier.width(168.dp), height = 27.dp, fontSize = LedgerStyle.typography.label.fontSize,
                containerColor = colors.surface, selectedContainerColor = colors.surfaceVariant,
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (type == TransactionType.TRANSFER) {
                RecordTransferAccounts(
                    accountName, destinationName, onAccountClick, onDestinationClick, onSwapAccounts,
                    Modifier.fillMaxSize().padding(bottom = footerHeight + footerLift).padding(horizontal = LedgerStyle.spacing.large).padding(top = 16.dp),
                )
            } else key(type) {
                RecordCategoryGrid(categories, selectedCategoryId, selectionColor, onCategorySelect, footerHeight + footerLift)
            }
            SurfaceBase(
                Modifier.align(Alignment.BottomCenter).offset(y = -footerLift).fillMaxWidth().onSizeChanged { footerHeight = with(density) { it.height.toDp() } },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), color = colors.surfaceVariant,
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium), verticalAlignment = Alignment.CenterVertically) {
                        if (type != TransactionType.TRANSFER) RecordCompactSelector(
                            accountName ?: textResources.getString(cn.turingzy.ledger.R.string.ui_select_account), MaterialSymbols.AccountBalanceWallet, onAccountClick,
                            Modifier.weight(1f, fill = false), textResources.getString(cn.turingzy.ledger.R.string.ui_select_account),
                        )
                        RecordCompactSelector(timeLabel, MaterialSymbols.Schedule, onTimeClick, description = textResources.getString(R.string.ui_select_time))
                        if (primaryAmountLabel != null) RecordCompactSelector(
                            label = primaryAmountLabel,
                            icon = null,
                            onClick = onEditPrimaryAmount,
                            modifier = Modifier.weight(1f, fill = false),
                            description = stringResource(R.string.ui_edit_primary_amount),
                            enabled = actionsEnabled,
                        )
                    }
                    SurfaceBase(shape = LedgerStyle.shapes.summaryCard, color = colors.surface) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(cn.turingzy.ledger.ui.component.biz.currencySymbolBiz(currency).trim(), fontSize = 20.sp, color = amountColor)
                                Text(
                                    expression.ifEmpty { "0.00" },
                                    modifier = Modifier.weight(1f).horizontalScroll(expressionScroll),
                                    style = TextStyle(fontSize = LedgerStyle.typography.inputAmount.fontSize, lineHeight = 28.sp, fontFeatureSettings = "tnum", color = amountColor),
                                    maxLines = 1, softWrap = false,
                                )
                            }
                            DividerBase()
                            BasicTextField(
                                value = note, onValueChange = onNoteChange, singleLine = true,
                                textStyle = TextStyle(fontSize = LedgerStyle.typography.bodySmall.fontSize, lineHeight = 18.sp, color = colors.onSurfaceVariant),
                                cursorBrush = SolidColor(colors.onSurface),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth().height(36.dp).semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_note) },
                                decorationBox = { field ->
                                    Box(Modifier.fillMaxSize().padding(start = 6.dp), contentAlignment = Alignment.CenterStart) {
                                        if (note.isEmpty()) Text(textResources.getString(cn.turingzy.ledger.R.string.ui_add_a_note), fontSize = LedgerStyle.typography.bodySmall.fontSize, lineHeight = 18.sp, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                                        field()
                                    }
                                },
                            )
                        }
                    }
                    if (transferRate != null) {
                        OutlinedTextField(transferRate, onTransferRateChange, label = { Text(transferLabel) }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            supportingText = { transferredAmount?.let { Text(textResources.getString(cn.turingzy.ledger.R.string.destination_amount, it)) } }, modifier = Modifier.fillMaxWidth())
                    }
                    error?.let { Text(it, color = colors.error, style = MaterialTheme.typography.bodySmall) }
                    BuiltInKeypad(
                        hasPendingCalculation = hasCalculation, actionEnabled = actionEnabled, keyHeight = keyHeight,
                        actionColor = selectionColor,
                        actionContentColor = if (type == TransactionType.TRANSFER) colors.scrim else colors.onPrimary,
                        onNumberClick = { digit ->
                            val last = recordLastOperand(expression)
                            if (expression.length < 80 && (!last.contains('.') || last.substringAfter('.').length < 2)) onExpressionChange(expression + digit)
                        },
                        onDotClick = {
                            val last = recordLastOperand(expression)
                            if (!last.contains('.') && expression.length < 78) onExpressionChange(expression + if (last.isEmpty()) "0." else ".")
                        },
                        onOperatorClick = { onExpressionChange(appendRecordOperator(expression, it)) },
                        onBackspaceClick = { onExpressionChange(expression.dropLast(1)) },
                        onClearClick = { onExpressionChange("") },
                        onActionClick = { focusManager.clearFocus(); onConfirm() },
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryAmountEditor(
    currency: String,
    originalAmount: Long,
    initialAmount: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
) {
    var amountText by rememberSaveable { mutableStateOf(initialAmount?.toFormattedAmount().orEmpty()) }
    val amountOverride = AmountFormatter.parseToCents(amountText)?.let { CurrencyAmountOverride(currency, it) }
    val validAmount = amountOverride?.takeIf { runCatching { it.rateFor(originalAmount) }.isSuccess }?.amount
    BaseAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.ui_edit_primary_amount)) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text(stringResource(R.string.ui_primary_amount_in_currency, currency)) },
                modifier = Modifier.testTag("primaryAmountInput"),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        onConfirm = { validAmount?.let(onConfirm) },
        confirmEnabled = validAmount != null,
    )
}

@Composable
private fun RecordCompactSelector(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String,
    enabled: Boolean = true,
) {
    Row(
        modifier.height(24.dp).clip(LedgerStyle.shapes.field).background(MaterialTheme.colorScheme.surface)
            .semantics { contentDescription = description }
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = LedgerStyle.spacing.medium),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall),
    ) {
        if (icon != null) Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, Modifier.weight(1f, fill = false), fontSize = LedgerStyle.typography.label.fontSize, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecordCategoryGrid(
    categories: List<CategoryWithIcon>,
    selectedId: Long,
    selectedColor: Color,
    onSelect: (Long) -> Unit,
    bottomPadding: Dp,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    if (categories.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(bottom = bottomPadding), contentAlignment = Alignment.Center) {
            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_no_categories_available), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        return
    }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = categories.indexOfFirst { it.category.id == selectedId }.coerceAtLeast(0) / 5 * 5)
    LazyVerticalGrid(
        GridCells.Fixed(5), Modifier.fillMaxSize().testTag("recordCategoryGrid"), state = gridState,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = bottomPadding + 12.dp),
        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraLarge),
    ) {
        items(categories, key = { it.category.id }) { item ->
            val chosen = item.category.id == selectedId
            Column(
                Modifier.semantics { selected = chosen }.clickable(
                    interactionSource = null, indication = null,
                ) { onSelect(item.category.id) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    Modifier.size(LedgerStyle.dimensions.compactIconButton).clip(CircleShape).background(if (chosen) selectedColor else colors.surface.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(item.icon.categoryGlyphBiz(), null, Modifier.size(22.dp), tint = if (chosen) colors.onPrimary else colors.onSurfaceVariant) }
                Text(item.category.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = LedgerStyle.typography.label.fontSize, color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun RecordTransferAccounts(
    source: String?, destination: String?, onSource: () -> Unit, onDestination: () -> Unit, onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        SurfaceBase(shape = LedgerStyle.shapes.summaryCard, color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.fillMaxWidth().padding(LedgerStyle.spacing.extraSmall),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransferAccountSelector(textResources.getString(cn.turingzy.ledger.R.string.ui_from), source, onSource, Modifier.weight(1f))
                IconButtonContentBase(
                    onClick = onSwap,
                    enabled = source != null && destination != null,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(MaterialSymbols.SwapHoriz, textResources.getString(cn.turingzy.ledger.R.string.ui_swap_accounts))
                }
                TransferAccountSelector(textResources.getString(cn.turingzy.ledger.R.string.ui_to), destination, onDestination, Modifier.weight(1f), alignment = Alignment.End)
            }
        }
    }
}

@Composable
private fun TransferAccountSelector(
    label: String,
    accountName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment.Horizontal = Alignment.Start,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(LedgerStyle.shapes.card)
            .semantics(mergeDescendants = true) { contentDescription = "选择${label}账户" }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall),
        horizontalAlignment = alignment,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        Text(
            accountName ?: textResources.getString(cn.turingzy.ledger.R.string.ui_select_account),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = if (accountName == null) colors.onSurfaceVariant else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 格式化交易时间戳为紧凑的展示标签。
 *
 * 格式规则：当天展示 "HH:mm"；当年非当天展示 "MM.dd HH:mm"；跨年展示 "yyyy.MM.dd HH:mm"。
 *
 * @param timestamp 交易时间戳（毫秒）。
 * @param zone 当前系统时区。
 * @param today 今天日期（默认根据时区获取）。
 * @return 格式化后的时间字符串。
 */
internal fun recordTimeLabel(timestamp: Long, zone: ZoneId, today: LocalDate = LocalDate.now(zone)): String {
    val value = Instant.ofEpochMilli(timestamp).atZone(zone)
    val date = value.toLocalDate()
    val time = value.format(DateTimeFormatter.ofPattern("HH:mm"))
    if (date == today) return time
    val label = date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "MM.dd" else "yyyy.MM.dd"))
    return "$label $time"
}
