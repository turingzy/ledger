package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.component.LedgerBottomSheet
import cn.turingzy.ledger.ui.component.LedgerSegmentedControl
import cn.turingzy.ledger.ui.component.categoryGlyph
import cn.turingzy.ledger.util.AmountFormatter
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
) {
    var expression by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
    var accountId by rememberSaveable { mutableLongStateOf(0L) }
    var destinationId by rememberSaveable { mutableLongStateOf(0L) }
    var categoryId by rememberSaveable { mutableLongStateOf(0L) }
    var transactionTime by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }
    var panel by rememberSaveable { mutableStateOf(RecordPanel.ENTRY) }
    val zone = ZoneId.systemDefault()
    val initialDateTime = Instant.ofEpochMilli(transactionTime).atZone(zone)
    var draftDay by rememberSaveable { mutableLongStateOf(initialDateTime.toLocalDate().toEpochDay()) }
    var draftHour by rememberSaveable { mutableStateOf(initialDateTime.hour.toString().padStart(2, '0')) }
    var draftMinute by rememberSaveable { mutableStateOf(initialDateTime.minute.toString().padStart(2, '0')) }
    val draftDate = LocalDate.ofEpochDay(draftDay)
    val draftTime = recordTimeMillis(draftDate, draftHour, draftMinute, zone)
    val focusManager = LocalFocusManager.current

    LaunchedEffect(accounts, accountId) {
        if (accounts.none { it.account.id == accountId }) accountId = accounts.firstOrNull()?.account?.id ?: 0L
        if (destinationId == accountId || accounts.none { it.account.id == destinationId }) {
            destinationId = accounts.firstOrNull { it.account.id != accountId }?.account?.id ?: 0L
        }
    }
    val matchingCategories = categories.filter { it.category.type == type.categoryType() && !it.category.isDeleted }.sortedBy { it.category.sortOrder }
    LaunchedEffect(categories, type) {
        if (matchingCategories.none { it.category.id == categoryId }) categoryId = matchingCategories.firstOrNull()?.category?.id ?: 0L
    }
    val hasPendingCalculation = expression.drop(1).any { it == '+' || it == '-' }
    val transaction = RecordFormInput(type, expression, accountId, destinationId, categoryId, transactionTime, note)
        .toTransactionOrNull(accounts, categories)
    val actionEnabled = if (hasPendingCalculation) AmountFormatter.parseToCents(evaluateMathExpression(expression)) != null else transaction != null

    fun showPanel(target: RecordPanel) {
        focusManager.clearFocus()
        panel = if (panel == target) RecordPanel.ENTRY else target
    }

    LedgerBottomSheet(
        title = "记一笔",
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        RecordTransactionContent(
            expression = expression, note = note, type = type, categories = matchingCategories,
            selectedCategoryId = categoryId,
            accountName = accounts.firstOrNull { it.account.id == accountId }?.account?.name ?: "选择账户",
            destinationName = accounts.firstOrNull { it.account.id == destinationId }?.account?.name ?: "选择转入账户",
            timeLabel = recordTimeLabel(transactionTime, zone),
            hasPendingCalculation = hasPendingCalculation, actionEnabled = actionEnabled,
            onTypeChange = { type = it; showPanel(RecordPanel.ENTRY) }, onCategorySelect = { categoryId = it },
            onExpressionChange = { expression = it }, onNoteChange = { note = it },
            onAccountClick = { showPanel(RecordPanel.ACCOUNT) },
            onDestinationClick = { showPanel(RecordPanel.DESTINATION) },
            onTimeClick = {
                if (panel != RecordPanel.DATE_TIME) {
                    val current = Instant.ofEpochMilli(transactionTime).atZone(zone)
                    draftDay = current.toLocalDate().toEpochDay()
                    draftHour = current.hour.toString().padStart(2, '0')
                    draftMinute = current.minute.toString().padStart(2, '0')
                }
                showPanel(RecordPanel.DATE_TIME)
            },
            onConfirmAction = {
                if (hasPendingCalculation) expression = evaluateMathExpression(expression)
                else transaction?.let { value ->
                    onConfirm(value.type, value.amount, value.accountId, value.toAccountId, value.categoryId, value.note, value.transactionTime)
                }
            },
            selectionExpanded = panel != RecordPanel.ENTRY,
            selectionContent = {
                if (panel == RecordPanel.ACCOUNT || panel == RecordPanel.DESTINATION) {
                    RecordInlineSelection(
                        title = if (panel == RecordPanel.DESTINATION) "转入账户" else "选择账户",
                        onClose = { showPanel(RecordPanel.ENTRY) },
                    ) {
                        RecordAccountChoices(
                            accounts = if (panel == RecordPanel.DESTINATION) accounts.filter { it.account.id != accountId } else accounts,
                            selectedId = if (panel == RecordPanel.DESTINATION) destinationId else accountId,
                            onSelect = {
                                if (panel == RecordPanel.DESTINATION) destinationId = it else accountId = it
                                showPanel(RecordPanel.ENTRY)
                            },
                        )
                    }
                } else if (panel == RecordPanel.DATE_TIME) {
                    RecordInlineSelection(
                        title = "选择时间",
                        onClose = { showPanel(RecordPanel.ENTRY) },
                        onConfirm = {
                            draftTime?.let { transactionTime = it; showPanel(RecordPanel.ENTRY) }
                        },
                        canConfirm = draftTime != null,
                    ) {
                        RecordDateTimePicker(
                            date = draftDate, hour = draftHour, minute = draftMinute,
                            onDateChange = { draftDay = it.toEpochDay() },
                            onHourChange = { draftHour = it }, onMinuteChange = { draftMinute = it },
                        )
                    }
                }
            },
        )
    }
}

@Composable
private fun RecordTransactionContent(
    expression: String,
    note: String,
    type: TransactionType,
    categories: List<CategoryWithIcon>,
    selectedCategoryId: Long,
    accountName: String,
    destinationName: String,
    timeLabel: String,
    hasPendingCalculation: Boolean,
    actionEnabled: Boolean,
    onTypeChange: (TransactionType) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onExpressionChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onAccountClick: () -> Unit,
    onDestinationClick: () -> Unit,
    onTimeClick: () -> Unit,
    onConfirmAction: () -> Unit,
    selectionExpanded: Boolean,
    selectionContent: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val expressionScroll = rememberScrollState()
    LaunchedEffect(expression) { expressionScroll.animateScrollTo(expressionScroll.maxValue) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("¥", style = MaterialTheme.typography.headlineSmall, color = colors.onSurfaceVariant)
                Text(
                    expression.ifEmpty { "0" }, modifier = Modifier.weight(1f).horizontalScroll(expressionScroll),
                    style = MaterialTheme.typography.headlineLarge.copy(fontFeatureSettings = "tnum"),
                    maxLines = 1, softWrap = false, color = colors.onSurface,
                )
            }
            LedgerSegmentedControl(
                labels = listOf("支出", "收入", "转账"), selectedIndex = TransactionType.entries.indexOf(type),
                onSelect = { focusManager.clearFocus(); onTypeChange(TransactionType.entries[it]) }, modifier = Modifier.fillMaxWidth(),
            )
            key(type) { RecordCategoryPages(categories, selectedCategoryId, onCategorySelect) }
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant.copy(alpha = 0.45f)) {
                Row(Modifier.fillMaxWidth().heightIn(min = 40.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Edit, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                    BasicTextField(
                        value = note, onValueChange = onNoteChange, singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface), cursorBrush = SolidColor(colors.onSurface),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier.weight(1f).semantics { contentDescription = "备注" },
                        decorationBox = { field ->
                            Box {
                                if (note.isEmpty()) Text("添加备注", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                field()
                            }
                        },
                    )
                }
            }

        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RecordSelector(
                label = if (type == TransactionType.TRANSFER) "转出：$accountName" else accountName,
                onClick = onAccountClick, modifier = Modifier.weight(1f, fill = false),
            )
            RecordSelector(timeLabel, onTimeClick)
        }
        if (type == TransactionType.TRANSFER) {
            RecordSelector("转入：$destinationName", onDestinationClick)
        }
        if (selectionExpanded) {
            Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).imePadding().verticalScroll(rememberScrollState())) {
                selectionContent()
            }
        } else {
            BuiltInKeypad(
                hasPendingCalculation = hasPendingCalculation, actionEnabled = actionEnabled,
                onNumberClick = { digit ->
                    focusManager.clearFocus()
                    val lastNumber = expression.split('+', '-').last()
                    if (expression.length < 80 && (!lastNumber.contains('.') || lastNumber.substringAfter('.', "").length < 2)) onExpressionChange(expression + digit)
                },
                onDotClick = {
                    focusManager.clearFocus()
                    val last = expression.split('+', '-').last()
                    if (!last.contains('.') && expression.length < 78) onExpressionChange(expression + if (last.isEmpty()) "0." else ".")
                },
                onOperatorClick = { op ->
                    focusManager.clearFocus()
                    if (expression.isNotEmpty() && expression.length < 80) onExpressionChange(if (expression.last() in "+-") expression.dropLast(1) + op else expression + op)
                },
                onBackspaceClick = { focusManager.clearFocus(); onExpressionChange(expression.dropLast(1)) },
                onActionClick = { focusManager.clearFocus(); onConfirmAction() },
            )
        }
    }
}

@Composable
private fun RecordInlineSelection(
    title: String,
    onClose: () -> Unit,
    onConfirm: (() -> Unit)? = null,
    canConfirm: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            if (onConfirm != null) {
                IconButton(onClick = onConfirm, enabled = canConfirm) {
                    Icon(Icons.Default.Check, "确认时间", tint = colors.tertiary.copy(alpha = if (canConfirm) 1f else 0.38f))
                }
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "收起$title", tint = colors.onSurfaceVariant)
            }
        }
        content()
    }
}

@Composable
private fun RecordSelector(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecordCategoryPages(categories: List<CategoryWithIcon>, selectedId: Long, onSelect: (Long) -> Unit) {
    val colors = MaterialTheme.colorScheme
    if (categories.isEmpty()) {
        Text("暂无对应分类，请先在分类管理中添加", Modifier.fillMaxWidth().padding(vertical = 16.dp), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        return
    }
    val pages = categories.chunked(6)
    val pager = rememberPagerState(pageCount = { pages.size })
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalPager(state = pager, pageSpacing = 12.dp) { page ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(6) { index ->
                    val item = pages[page].getOrNull(index)
                    if (item == null) Box(Modifier.weight(1f))
                    else {
                        val chosen = item.category.id == selectedId
                        Column(
                            Modifier.weight(1f).semantics { selected = chosen }.clickable(
                                interactionSource = remember { MutableInteractionSource() }, indication = null,
                            ) { onSelect(item.category.id) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Box(
                                Modifier.size(36.dp).clip(CircleShape).background(colors.surfaceVariant.copy(alpha = 0.45f))
                                    .then(if (chosen) Modifier.border(1.dp, colors.onSurface, CircleShape) else Modifier),
                                contentAlignment = Alignment.Center,
                            ) { Icon(item.icon.categoryGlyph(), null, Modifier.size(20.dp), tint = colors.onSurface) }
                            Text(item.category.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp, color = colors.onSurface)
                        }
                    }
                }
            }
        }
        if (pages.size > 1) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                repeat(pages.size) { index -> Box(Modifier.size(4.dp).clip(CircleShape).background(if (pager.currentPage == index) colors.onSurface else colors.outlineVariant)) }
            }
        }
    }
}

@Composable
private fun RecordAccountChoices(accounts: List<AccountWithTemplate>, selectedId: Long, onSelect: (Long) -> Unit) {
    if (accounts.isEmpty()) {
        Text("暂无可选账户，请先在钱包中添加", Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        LazyColumn(Modifier.heightIn(max = 180.dp).padding(horizontal = 16.dp)) {
            items(accounts, key = { it.account.id }) { item ->
                Row(Modifier.fillMaxWidth().clickable { onSelect(item.account.id) }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.account.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    if (item.account.id == selectedId) Icon(Icons.Default.Check, "已选择", tint = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

private fun recordTimeLabel(timestamp: Long, zone: ZoneId): String {
    val value = Instant.ofEpochMilli(timestamp).atZone(zone)
    val date = value.toLocalDate()
    val today = LocalDate.now(zone)
    val dateLabel = if (date == today) "今天" else date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "MM.dd" else "yyyy.MM.dd"))
    return "$dateLabel ${value.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}
