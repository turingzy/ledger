package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.turingzy.ledger.model.AccountType

/**
 * 添加账户对话框（已升级为抽屉样式的 [AddAccountSheet]，保留作为兼容接口）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountDialog(
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, type: AccountType, initialBalance: Long, cardNumber: String?, includeInTotal: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    AddAccountSheet(
        onDismissRequest = onDismissRequest,
        onConfirm = { name, type, initialBalance, cardNumber, includeInTotal, _, _ ->
            onConfirm(name, type, initialBalance, cardNumber, includeInTotal)
        },
        modifier = modifier,
    )
}
