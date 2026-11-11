package cn.turingzy.ledger.ui.feature.ledger

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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.HorizontalDivider
import cn.turingzy.ledger.ui.component.LedgerIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
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
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.SummaryCardIconButton
import cn.turingzy.ledger.ui.component.categoryGlyph
import cn.turingzy.ledger.ui.theme.IosYellow
import cn.turingzy.ledger.util.AmountFormatter
import cn.turingzy.ledger.util.toFormattedAmount
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class RecordPanel { ENTRY, ACCOUNT, DESTINATION, DATE_TIME }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTransactionSheet(
    sheetState: SheetState,
    categories: List<CategoryWithIcon>,
    accounts: List<AccountWithTemplate>,
    onDismissRequest: () -> Unit,
    onConfirm: (
        type: TransactionType, amount: Long, accountId: Long, toAccountId: Long?,
        categoryId: Long, note: String?, transactionTime: Long,
    ) -> Unit,
    modifier: Modifier = Modifier,
    initialTransaction: Transaction? = null,
    title: String = "记一笔",
    saving: Boolean = false,
    error: String? = null,
    sheetHeight: Dp = 520.dp,
) {
    val fixedHeight = remember { sheetHeight }
    var expression by rememberSaveable(initialTransaction?.id) { mutableStateOf(initialTransaction?.amount?.toFormattedAmount().orEmpty()) }
    var note by rememberSaveable(initialTransaction?.id) { mutableStateOf(initialTransaction?.note.orEmpty()) }
    var type by rememberSaveable(initialTransaction?.id) { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var accountId by rememberSaveable(initialTransaction?.id) { mutableLongStateOf(initialTransaction?.accountId ?: 0L) }
    var destinationId by rememberSaveable(initialTransaction?.id) { mutableLongStateOf(initialTransaction?.toAccountId ?: 0L) }
    var categoryId by rememberSaveable(initialTransaction?.id) { mutableLongStateOf(initialTransaction?.categoryId ?: 0L) }
    var transactionTime by rememberSaveable(initialTransaction?.id) { mutableLongStateOf(initialTransaction?.transactionTime ?: System.currentTimeMillis()) }
    var panel by rememberSaveable { mutableStateOf(RecordPanel.ENTRY) }
    val focusManager = LocalFocusManager.current
    val zone = ZoneId.systemDefault()
    val matchingCategories = remember(categories, type) { selectableRecordCategories(categories, type) }
    LaunchedEffect(matchingCategories, type) {
        if (type == TransactionType.TRANSFER && matchingCategories.none { it.category.id == categoryId }) {
            categoryId = matchingCategories.firstOrNull()?.category?.id ?: 0L
        }
    }
    val hasCalculation = expression.drop(1).any { it in "+-×÷" }
    val canEvaluate = !saving && hasCalculation && AmountFormatter.parseToCents(evaluateMathExpression(expression)) != null
    val transaction = RecordFormInput(type, expression, accountId, destinationId, categoryId, transactionTime, note)
        .toTransactionOrNull(accounts, categories)

    fun openPanel(target: RecordPanel) {
        if (saving) return
        focusManager.clearFocus()
        panel = target
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest, sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        RecordTransactionContent(
            // Keep the outer constraints at window size so Material computes the correct expanded anchor.
            modifier = Modifier.height(fixedHeight).testTag("recordDrawer").navigationBarsPadding(),
            expression = expression, note = note, type = type, categories = matchingCategories,
            selectedCategoryId = categoryId,
            accountName = accounts.firstOrNull { it.account.id == accountId }?.account?.name,
            destinationName = accounts.firstOrNull { it.account.id == destinationId }?.account?.name,
            timeLabel = recordTimeLabel(transactionTime, zone),
            title = title, error = error,
            hasCalculation = hasCalculation,
            actionEnabled = if (hasCalculation) canEvaluate else !saving && transaction != null,
            onDismiss = onDismissRequest,
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
                    if (hasCalculation) expression = evaluateMathExpression(expression)
                    else transaction?.let { value ->
                        onConfirm(value.type, value.amount, value.accountId, value.toAccountId, value.categoryId, value.note, value.transactionTime)
                    }
                }
            },
            keyHeight = if (fixedHeight < 540.dp) 40.dp else 48.dp,
        )
    }
    if (panel == RecordPanel.ACCOUNT || panel == RecordPanel.DESTINATION) {
        val destination = panel == RecordPanel.DESTINATION
        RecordAccountSheet(
            title = if (destination) "选择转入账户" else if (type == TransactionType.TRANSFER) "选择转出账户" else "选择账户",
            accounts = accounts.filter { !it.account.isDeleted && (!destination || it.account.id != accountId) },
            selectedId = if (destination) destinationId else accountId,
            transactionType = type,
            height = fixedHeight * 0.65f,
            onDismiss = { panel = RecordPanel.ENTRY },
            onSelect = { id ->
                if (destination) destinationId = id else {
                    accountId = id
                    if (destinationId == id) destinationId = 0L
                }
                panel = RecordPanel.ENTRY
            },
        )
    }
    if (panel == RecordPanel.DATE_TIME) {
        RecordDateTimeDialog(
            transactionTime, onDismiss = { panel = RecordPanel.ENTRY },
            onConfirm = { transactionTime = it; panel = RecordPanel.ENTRY },
        )
    }
}

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
    title: String,
    hasCalculation: Boolean,
    actionEnabled: Boolean,
    onDismiss: () -> Unit,
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
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val expressionScroll = rememberScrollState()
    var footerHeight by remember { mutableStateOf(0.dp) }
    val selectionColor = when (type) {
        TransactionType.EXPENSE -> colors.error
        TransactionType.INCOME -> colors.tertiary
        TransactionType.TRANSFER -> IosYellow
    }
    val amountColor = selectionColor
    LaunchedEffect(expression) { expressionScroll.animateScrollTo(expressionScroll.maxValue) }
    Column(modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            SummaryCardIconButton(
                MaterialSymbols.Close, "关闭$title", onDismiss, showPressFeedback = false,
                modifier = Modifier.align(Alignment.CenterStart), containerColor = colors.surface, contentColor = colors.onSurface,
            )
            LedgerSegmentedControl(
                listOf("支出", "收入", "转账"), TransactionType.entries.indexOf(type),
                { onTypeChange(TransactionType.entries[it]) },
                modifier = Modifier.width(168.dp), height = 27.dp, fontSize = 12.sp,
                containerColor = colors.surface, selectedContainerColor = colors.surfaceVariant,
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (type == TransactionType.TRANSFER) {
                RecordTransferAccounts(
                    accountName, destinationName, onAccountClick, onDestinationClick, onSwapAccounts,
                    Modifier.fillMaxSize().padding(bottom = footerHeight).padding(horizontal = 12.dp).padding(top = 16.dp),
                )
            } else key(type) {
                RecordCategoryGrid(categories, selectedCategoryId, selectionColor, onCategorySelect, footerHeight)
            }
            Surface(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().onSizeChanged { footerHeight = with(density) { it.height.toDp() } },
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp), color = colors.surfaceVariant,
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (type != TransactionType.TRANSFER) RecordCompactSelector(
                            accountName ?: "选择账户", MaterialSymbols.AccountBalanceWallet, onAccountClick,
                            Modifier.weight(1f, fill = false), "选择账户",
                        )
                        RecordCompactSelector(timeLabel, MaterialSymbols.Schedule, onTimeClick, description = "选择时间")
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = colors.surface) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
                            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("¥", fontSize = 20.sp, color = amountColor)
                                Text(
                                    expression.ifEmpty { "0.00" },
                                    modifier = Modifier.weight(1f).horizontalScroll(expressionScroll),
                                    style = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontFeatureSettings = "tnum", color = amountColor),
                                    maxLines = 1, softWrap = false,
                                )
                            }
                            HorizontalDivider(color = colors.outlineVariant)
                            BasicTextField(
                                value = note, onValueChange = onNoteChange, singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant),
                                cursorBrush = SolidColor(colors.onSurface),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth().height(36.dp).semantics { contentDescription = "备注" },
                                decorationBox = { field ->
                                    Box(Modifier.fillMaxSize().padding(start = 6.dp), contentAlignment = Alignment.CenterStart) {
                                        if (note.isEmpty()) Text("填写备注", fontSize = 13.sp, lineHeight = 18.sp, color = colors.onSurfaceVariant.copy(alpha = 0.6f))
                                        field()
                                    }
                                },
                            )
                        }
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
private fun RecordCompactSelector(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String,
) {
    Row(
        modifier.height(24.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface)
            .semantics { contentDescription = description }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, Modifier.weight(1f, fill = false), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val colors = MaterialTheme.colorScheme
    if (categories.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(bottom = bottomPadding), contentAlignment = Alignment.Center) {
            Text("暂无可选分类", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        return
    }
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = categories.indexOfFirst { it.category.id == selectedId }.coerceAtLeast(0) / 5 * 5)
    LazyVerticalGrid(
        GridCells.Fixed(5), Modifier.fillMaxSize().testTag("recordCategoryGrid"), state = gridState,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = bottomPadding + 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(categories, key = { it.category.id }) { item ->
            val chosen = item.category.id == selectedId
            Column(
                Modifier.semantics { selected = chosen }.clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = LocalIndication.current,
                ) { onSelect(item.category.id) },
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(if (chosen) selectedColor else colors.surface.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(item.icon.categoryGlyph(), null, Modifier.size(22.dp), tint = if (chosen) colors.onPrimary else colors.onSurfaceVariant) }
                Text(item.category.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, color = colors.onSurface)
            }
        }
    }
}

@Composable
private fun RecordTransferAccounts(
    source: String?, destination: String?, onSource: () -> Unit, onDestination: () -> Unit, onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
            Row(
                Modifier.fillMaxWidth().padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransferAccountSelector("转出", source, onSource, Modifier.weight(1f))
                IconButton(
                    onClick = onSwap,
                    enabled = source != null && destination != null,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(MaterialSymbols.SwapHoriz, "交换转出转入账户")
                }
                TransferAccountSelector("转入", destination, onDestination, Modifier.weight(1f))
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
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier.clip(RoundedCornerShape(16.dp))
            .semantics(mergeDescendants = true) { contentDescription = "选择${label}账户" }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        Text(
            accountName ?: "选择账户",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = if (accountName == null) colors.onSurfaceVariant else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun recordTimeLabel(timestamp: Long, zone: ZoneId, today: LocalDate = LocalDate.now(zone)): String {
    val value = Instant.ofEpochMilli(timestamp).atZone(zone)
    val date = value.toLocalDate()
    val time = value.format(DateTimeFormatter.ofPattern("HH:mm"))
    if (date == today) return time
    val label = date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "MM.dd" else "yyyy.MM.dd"))
    return "$label $time"
}
