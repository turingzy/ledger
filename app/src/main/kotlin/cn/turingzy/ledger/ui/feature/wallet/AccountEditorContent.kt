package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
internal fun AccountEditorContent(
    account: Account?,
    templates: List<CardTemplate>,
    onClose: () -> Unit,
    onSave: (Account) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable(account?.id) { mutableStateOf(account?.name.orEmpty()) }
    var type by rememberSaveable(account?.id) { mutableStateOf(account?.type ?: AccountType.DEBIT) }
    var balance by rememberSaveable(account?.id) { mutableStateOf(account?.initialBalance?.toFormattedAmount().orEmpty()) }
    var cardNumber by rememberSaveable(account?.id) { mutableStateOf(account?.cardNumber.orEmpty()) }
    var currency by rememberSaveable(account?.id) { mutableStateOf(account?.currency ?: "CNY") }
    var templateId by rememberSaveable(account?.id) { mutableStateOf(account?.templateId) }
    var includeInTotal by rememberSaveable(account?.id) { mutableStateOf(account?.includeInTotal ?: true) }
    var sortOrder by rememberSaveable(account?.id) { mutableStateOf(account?.sortOrder?.toString().orEmpty()) }
    val input = AccountFormInput(name, type, balance, cardNumber, currency, templateId, includeInTotal, sortOrder)
    val result = if (account == null) input.toAccountOrNull(templates) else input.toUpdatedAccountOrNull(account, templates)
    val activeTemplates = remember(templates) { templates.filterNot { it.isDeleted } }
    val currencies = accountCurrencyOptions
    val focusManager = LocalFocusManager.current
    val colors = MaterialTheme.colorScheme

    AccountSheetContent(
        title = if (account == null) "添加账户" else "账户详情",
        closeDescription = if (account == null) "取消添加账户" else "关闭账户详情",
        onClose = onClose,
        saveEnabled = result != null,
        onSave = {
            val value = result ?: return@AccountSheetContent
            focusManager.clearFocus()
            onSave(value)
        },
        modifier = modifier,
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("计入总资产", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                Switch(
                    checked = includeInTotal,
                    onCheckedChange = { includeInTotal = it },
                    modifier = Modifier.scale(0.72f).semantics { contentDescription = "计入总资产" },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = colors.tertiary,
                        checkedThumbColor = colors.surface,
                        checkedBorderColor = colors.tertiary,
                    ),
                )
            }
        }
        AccountFormTextField(
            label = "账户名称", value = name, onValueChange = { name = it },
            placeholder = "例如：工资卡",
        )
        AccountFormDropdown(
            label = "账户类型", value = type.displayName(), options = AccountType.entries,
            optionLabel = { it.displayName() }, onSelect = { type = it },
        )
        if (account == null) {
            AccountFormDropdown(
                label = "币种", value = currency, options = currencies,
                optionLabel = { it }, onSelect = { currency = it },
            )
        } else {
            AccountFormTextField(
                label = "币种", value = account.currency, onValueChange = {}, readOnly = true,
            )
        }
        AccountFormTextField(
            label = if (type == AccountType.CREDIT) "初始欠款" else "初始余额",
            value = balance, onValueChange = { balance = it }, placeholder = "0.00", suffix = input.currencyCode,
            error = if (input.balanceCents == null) "请输入有效金额" else null,
            readOnly = account != null,
            keyboardType = KeyboardType.Decimal,
        )
        if (account != null) {
            AccountFormTextField(
                label = if (account.type == AccountType.CREDIT) "当前欠款" else "当前余额",
                value = account.currentBalance.toFormattedAmount(useGrouping = true),
                onValueChange = {}, suffix = account.currency, readOnly = true,
            )
        }
        if (type == AccountType.DEBIT || type == AccountType.CREDIT) {
            AccountFormTextField(
                label = "尾号", value = cardNumber, onValueChange = { cardNumber = it },
                error = if (!input.cardNumberValid) "请输入不超过 19 位的数字" else null,
                keyboardType = KeyboardType.Number,
            )
        }
        AccountFormDropdown(
            label = "卡面模板",
            value = if (templateId == null) "默认卡面" else activeTemplates.firstOrNull { it.id == templateId }?.name ?: "请重新选择模板",
            options = listOf<CardTemplate?>(null) + activeTemplates,
            optionLabel = { it?.name ?: "默认卡面" }, onSelect = { templateId = it?.id },
        )
        AccountFormTextField(
            label = "排序", value = sortOrder, onValueChange = { sortOrder = it },
            placeholder = "数字越小越靠前",
            error = if (input.order == null) "请输入有效的非负整数" else null,
            keyboardType = KeyboardType.Number, imeAction = ImeAction.Done,
        )
    }
}
