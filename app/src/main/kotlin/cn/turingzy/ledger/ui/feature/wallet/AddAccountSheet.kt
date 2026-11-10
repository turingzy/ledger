package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheet(
    onDismissRequest: () -> Unit,
    onConfirm: (
        name: String,
        type: AccountType,
        initialBalance: Long,
        cardNumber: String?,
        includeInTotal: Boolean,
        currency: String,
        templateId: Long?,
        sortOrder: Int,
    ) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    templates: List<CardTemplate> = emptyList(),
) {
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
        AddAccountSheetContent(onDismissRequest, onConfirm, templates = templates)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountSheetContent(
    onDismissRequest: () -> Unit,
    onConfirm: (
        name: String,
        type: AccountType,
        initialBalance: Long,
        cardNumber: String?,
        includeInTotal: Boolean,
        currency: String,
        templateId: Long?,
        sortOrder: Int,
    ) -> Unit,
    modifier: Modifier = Modifier,
    templates: List<CardTemplate> = emptyList(),
) {
    var name by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(AccountType.DEBIT) }
    var balance by rememberSaveable { mutableStateOf("") }
    var cardNumber by rememberSaveable { mutableStateOf("") }
    var currency by rememberSaveable { mutableStateOf("CNY") }
    var templateId by rememberSaveable { mutableStateOf<Long?>(null) }
    var includeInTotal by rememberSaveable { mutableStateOf(true) }
    var sortOrder by rememberSaveable { mutableStateOf("") }
    val input = AccountFormInput(name, type, balance, cardNumber, currency, templateId, includeInTotal, sortOrder)
    val account = input.toAccountOrNull(templates)
    val colors = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val activeTemplates = templates.filterNot { it.isDeleted }

    Column(modifier.fillMaxWidth().imePadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onDismissRequest) {
                Icon(MaterialSymbols.Close, "取消添加账户", Modifier.size(24.dp), tint = colors.onSurfaceVariant)
            }
            Text("添加账户", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            AccountSaveButton(
                enabled = account != null,
                onClick = {
                    val value = account ?: return@AccountSaveButton
                    focusManager.clearFocus()
                    onConfirm(
                        value.name, value.type, value.initialBalance, value.cardNumber,
                        value.includeInTotal, value.currency, value.templateId, value.sortOrder,
                    )
                },
            )
        }
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("账户名称") },
                placeholder = { Text("例如：工资卡、微信零钱") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            AccountFormDropdown(
                label = "账户类型",
                value = type.displayName(),
                options = AccountType.entries,
                optionLabel = { it.displayName() },
                onSelect = { type = it },
            )
            OutlinedTextField(
                value = balance,
                onValueChange = { balance = it },
                label = { Text(if (type == AccountType.CREDIT) "初始欠款" else "初始余额") },
                placeholder = { Text("0.00") },
                suffix = { Text(input.currencyCode) },
                isError = input.balanceCents == null,
                supportingText = { Text(if (input.balanceCents == null) "请输入有效金额" else "请确认初始金额，创建后当前余额不可手动修改") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = currency,
                onValueChange = { currency = it.uppercase(Locale.ROOT) },
                label = { Text("币种") },
                placeholder = { Text("CNY") },
                isError = input.currencyCode !in accountCurrencyCodes,
                supportingText = { Text(if (input.currencyCode in accountCurrencyCodes) "创建后币种不可修改，如 CNY、USD、HKD" else "请输入有效的币种代码") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (type == AccountType.DEBIT || type == AccountType.CREDIT) {
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("卡号或尾号（选填）") },
                    isError = !input.cardNumberValid,
                    supportingText = if (!input.cardNumberValid) ({ Text("请输入不超过 19 位的数字") }) else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            AccountFormDropdown(
                label = "卡面模板",
                value = if (templateId == null) "默认卡面" else activeTemplates.firstOrNull { it.id == templateId }?.name ?: "请重新选择模板",
                options = listOf<CardTemplate?>(null) + activeTemplates,
                optionLabel = { it?.name ?: "默认卡面" },
                onSelect = { templateId = it?.id },
            )
            OutlinedTextField(
                value = sortOrder,
                onValueChange = { sortOrder = it },
                label = { Text("排序") },
                placeholder = { Text("0") },
                isError = input.order == null,
                supportingText = { Text(if (input.order == null) "请输入有效的非负整数" else "数字越小越靠前") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("计入总资产", style = MaterialTheme.typography.bodyLarge, color = colors.onSurface)
                Switch(checked = includeInTotal, onCheckedChange = { includeInTotal = it })
            }
        }
    }
}


@Preview(showBackground = true, heightDp = 900)
@Composable
private fun AddAccountSheetContentPreview() {
    LedgerTheme {
        AddAccountSheetContent(onDismissRequest = {}, onConfirm = { _, _, _, _, _, _, _, _ -> })
    }
}
