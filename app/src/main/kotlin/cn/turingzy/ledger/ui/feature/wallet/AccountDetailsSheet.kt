package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.foundation.LocalIndication
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import cn.turingzy.ledger.ui.component.LedgerIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme
import cn.turingzy.ledger.util.toFormattedAmount
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailsSheet(
    account: Account,
    onDismissRequest: () -> Unit,
    onSave: (Account) -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val scope = rememberCoroutineScope()
    fun dismiss() {
        scope.launch {
            sheetState.hide()
            if (!sheetState.isVisible) onDismissRequest()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp).height(5.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            )
        },
        modifier = modifier,
    ) {
        AccountDetailsContent(
            account = account,
            templates = templates,
            onClose = ::dismiss,
            onSave = {
                onSave(it)
                dismiss()
            },
        )
    }
}

@Composable
fun AccountDetailsContent(
    account: Account,
    onClose: () -> Unit,
    onSave: (Account) -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
) {
    var name by rememberSaveable(account.id) { mutableStateOf(account.name) }
    var type by rememberSaveable(account.id) { mutableStateOf(account.type) }
    var initialBalance by rememberSaveable(account.id) { mutableStateOf(account.initialBalance.toFormattedAmount()) }
    var cardNumber by rememberSaveable(account.id) { mutableStateOf(account.cardNumber.orEmpty()) }
    var templateId by rememberSaveable(account.id) { mutableStateOf(account.templateId) }
    var includeInTotal by rememberSaveable(account.id) { mutableStateOf(account.includeInTotal) }
    var sortOrder by rememberSaveable(account.id) { mutableStateOf(account.sortOrder.toString()) }
    val input = AccountFormInput(name, type, initialBalance, cardNumber, account.currency, templateId, includeInTotal, sortOrder)
    val updatedAccount = input.toUpdatedAccountOrNull(account, templates)
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val activeTemplates = templates.filterNot { it.isDeleted }

    Column(modifier.fillMaxWidth().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(showPressFeedback = false, onClick = onClose) {
                Icon(MaterialSymbols.Close, "关闭账户详情", Modifier.size(24.dp), tint = colors.onSurfaceVariant)
            }
            Text("账户详情", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            AccountSaveButton(
                enabled = updatedAccount != null,
                onClick = {
                    val value = updatedAccount ?: return@AccountSaveButton
                    focusManager.clearFocus()
                    onSave(value)
                },
            )
        }
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(shape = RoundedCornerShape(16.dp), color = colors.surfaceVariant.copy(alpha = 0.45f)) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    AccountInformationField(divider = false) {
                        AccountInlineTextField(
                            label = "账户名称",
                            value = name,
                            onValueChange = { name = it },
                            error = if (name.isBlank()) "请输入账户名称" else null,
                        )
                    }
                    AccountInformationField {
                        AccountInlineChoice(
                            label = "账户类型",
                            value = type.displayName(),
                            options = AccountType.entries,
                            optionLabel = { it.displayName() },
                            onSelect = { type = it },
                        )
                    }
                    if (type == AccountType.DEBIT || type == AccountType.CREDIT) {
                        AccountInformationField {
                            AccountInlineTextField(
                                label = "卡号或尾号",
                                value = cardNumber,
                                onValueChange = { cardNumber = it },
                                keyboardType = KeyboardType.Number,
                                placeholder = "未填写",
                                error = if (!input.cardNumberValid) "请输入不超过 19 位的数字" else null,
                            )
                        }
                    }
                    AccountInformationField { AccountDetailRow("币种（不可修改）", account.currency) }
                    AccountInformationField {
                        AccountDetailRow(
                            if (account.type == AccountType.CREDIT) "当前欠款（不可修改）" else "当前余额（不可修改）",
                            "${account.currentBalance.toFormattedAmount(useGrouping = true)} ${account.currency}",
                        )
                    }
                    AccountInformationField {
                        AccountInlineTextField(
                            label = if (type == AccountType.CREDIT) "初始欠款" else "初始余额",
                            value = initialBalance,
                            onValueChange = { initialBalance = it },
                            keyboardType = KeyboardType.Decimal,
                            placeholder = "0.00",
                            suffix = account.currency,
                            error = if (input.balanceCents == null) "请输入有效金额" else null,
                        )
                    }
                    AccountInformationField {
                        AccountInlineChoice(
                            label = "计入总资产",
                            value = if (includeInTotal) "是" else "否",
                            options = listOf(true, false),
                            optionLabel = { if (it) "是" else "否" },
                            onSelect = { includeInTotal = it },
                        )
                    }
                    AccountInformationField {
                        AccountInlineChoice(
                            label = "卡面模板",
                            value = if (templateId == null) "默认卡面" else activeTemplates.firstOrNull { it.id == templateId }?.name ?: "请重新选择模板",
                            options = listOf<CardTemplate?>(null) + activeTemplates,
                            optionLabel = { it?.name ?: "默认卡面" },
                            onSelect = { templateId = it?.id },
                        )
                    }
                    AccountInformationField {
                        AccountInlineTextField(
                            label = "排序",
                            value = sortOrder,
                            onValueChange = { sortOrder = it },
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                            placeholder = "0",
                            error = if (input.order == null) "请输入有效的非负整数" else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountInformationField(divider: Boolean = true, content: @Composable () -> Unit) {
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Box(Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun AccountInlineTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    placeholder: String = "",
    suffix: String? = null,
    error: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(
        modifier.fillMaxWidth().clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current,
        ) {
            focusRequester.requestFocus()
            keyboard?.show()
        }.padding(vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    textAlign = TextAlign.End,
                    color = if (error == null) colors.onSurface else colors.error,
                ),
                cursorBrush = SolidColor(colors.tertiary),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = { focusManager.clearFocus() },
                ),
                modifier = Modifier.weight(1f).focusRequester(focusRequester)
                    .semantics { contentDescription = label },
                decorationBox = { innerTextField ->
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                        }
                        innerTextField()
                    }
                },
            )
            if (suffix != null) Text(suffix, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        }
        if (error != null) {
            Text(
                error,
                modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = colors.error,
            )
        }
    }
}

@Composable
private fun <T> AccountInlineChoice(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme
    Box(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                focusManager.clearFocus()
                expanded = true
            }.padding(vertical = 14.dp).heightIn(min = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            Text(
                value,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface,
            )
            Icon(MaterialSymbols.ExpandMore, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AccountDetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp).heightIn(min = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AccountDetailsPreview() {
    LedgerTheme {
        AccountDetailsContent(
            account = Account(id = 1L, name = "工资卡", type = AccountType.DEBIT, currentBalance = 123456L, cardNumber = "1234"),
            onClose = {},
            onSave = {},
        )
    }
}
