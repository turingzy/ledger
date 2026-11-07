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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    var balanceText by remember {
        mutableStateOf(account.currentBalance.toFormattedAmount(showSymbol = false))
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = "校准余额 - ${account.name}")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("最新实际余额 (元)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val cents = AmountFormatter.parseToCents(balanceText)
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
