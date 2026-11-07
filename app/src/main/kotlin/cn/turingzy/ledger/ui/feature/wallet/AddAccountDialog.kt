package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.util.AmountFormatter

@Composable
fun AddAccountDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, type: AccountType, initialBalance: Long, cardNumber: String?, includeInTotal: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.DEBIT) }
    var balanceText by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var includeInTotal by remember { mutableStateOf(true) }

    val isNameValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "添加新账户")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账户名称") },
                    placeholder = { Text("例如：工商银行储蓄卡") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "账户类型",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                ) {
                    AccountType.entries.forEach { type ->
                        val label = when (type) {
                            AccountType.DEBIT -> "储蓄卡 / 借记卡"
                            AccountType.CREDIT -> "信用卡 / 信用额度"
                            AccountType.WALLET -> "电子钱包 (微信/支付宝)"
                            AccountType.CASH -> "现金钱包"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = null,
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("初始金额 (元)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { if (it.length <= 4) cardNumber = it },
                    label = { Text("卡号尾号 (可选，后4位)") },
                    placeholder = { Text("6821") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "计入总资产看板",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "关闭后此账户不计入净资产统计",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = includeInTotal,
                        onCheckedChange = { includeInTotal = it },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isNameValid) {
                        val cents = AmountFormatter.parseToCents(balanceText) ?: 0L
                        onConfirm(name, selectedType, cents, cardNumber, includeInTotal)
                    }
                },
                enabled = isNameValid,
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        },
        modifier = modifier,
    )
}
