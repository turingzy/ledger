package cn.turingzy.ledger.ui.component.biz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.theme.LedgerStyle

@Composable
fun BusinessAccountIcon(account: Account, modifier: Modifier = Modifier) {
    val colors = LedgerStyle.colors
    val background = when (account.displayAccent()) {
        AccountAccent.Primary -> colors.accountDebit
        AccountAccent.Income -> colors.accountWallet
        AccountAccent.Credit -> colors.accountCredit
        AccountAccent.Cash -> colors.accountCash
    }
    val icon = when (account.type) {
        AccountType.DEBIT -> MaterialSymbols.AccountBalance
        AccountType.CREDIT -> MaterialSymbols.Contactless
        AccountType.WALLET -> MaterialSymbols.AccountBalanceWallet
        AccountType.CASH -> MaterialSymbols.Payments
    }
    Box(
        modifier.size(LedgerStyle.dimensions.compactIconButton).clip(LedgerStyle.shapes.pill).background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(LedgerStyle.icons.inline), tint = MaterialTheme.colorScheme.onPrimary)
    }
}

internal enum class AccountAccent { Primary, Income, Credit, Cash }

internal fun Account.displayAccent(): AccountAccent = when {
    name.contains("支付宝") -> AccountAccent.Primary
    name.contains("微信") -> AccountAccent.Income
    type == AccountType.DEBIT -> AccountAccent.Primary
    type == AccountType.CREDIT -> AccountAccent.Credit
    type == AccountType.CASH -> AccountAccent.Cash
    else -> AccountAccent.Income
}

@Composable
internal fun BusinessTypeIcon(
    imageVector: ImageVector,
    type: TransactionType,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val colors = LedgerStyle.colors
    Box(
        modifier.size(LedgerStyle.dimensions.compactIconButton).clip(LedgerStyle.shapes.pill)
            .background(colors.transaction(type)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector,
            contentDescription = contentDescription,
            modifier = Modifier.size(LedgerStyle.icons.inline),
            tint = colors.onTransaction(type),
        )
    }
}
