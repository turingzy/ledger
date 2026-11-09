package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.util.AmountFormatter
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
fun AdjustBalanceDialog(
    account: Account,
    onDismissRequest: () -> Unit,
    onConfirm: (newBalance: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var balanceText by rememberSaveable(account.id) {
        mutableStateOf(account.currentBalance.toFormattedAmount(showSymbol = false))
    }

    val parsedBalance = AmountFormatter.parseToCents(balanceText)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "校准余额")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            ) {
                Text(account.name, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("最新实际余额 (元)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = balanceText.isNotBlank() && parsedBalance == null,
                    supportingText = {
                        Text(if (balanceText.isNotBlank() && parsedBalance == null) "请输入有效金额" else "当前余额：${account.currentBalance.toFormattedAmount(showSymbol = true)}")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsedBalance != null,
                onClick = {
                    val cents = parsedBalance
                    if (cents != null) {
                        onConfirm(cents)
                    }
                },
            ) {
                Text("确定")
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
