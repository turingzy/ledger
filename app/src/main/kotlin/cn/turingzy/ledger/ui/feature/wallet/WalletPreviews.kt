package cn.turingzy.ledger.ui.feature.wallet

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme

private fun previewAccounts(): List<AccountWithTemplate> = listOf(
    Account(1L, "银行卡", AccountType.DEBIT, currentBalance = 1234500L, cardNumber = "6222000012345678"),
    Account(2L, "电子钱包", AccountType.WALLET, currentBalance = 56000L),
    Account(3L, "信用卡", AccountType.CREDIT, currentBalance = -230000L, cardNumber = "4392000012349012"),
    Account(4L, "现金", AccountType.CASH, currentBalance = 50000L),
).map { AccountWithTemplate(it, null) }

internal class WalletPreviewProvider : PreviewParameterProvider<WalletUiState> {
    override val values: Sequence<WalletUiState> = sequence {
        val accounts = previewAccounts()
        val empty = WalletUiState.Success(0L, 0L, emptyList())
        val populated = empty.copy(
            totalAsset = 1110500L, totalDebt = 230000L, accounts = accounts,
            currencyTotals = listOf(WalletCurrencyTotals("CNY", assets = 1110500L, expense = 25000L, income = 600000L)),
        )
        yield(WalletUiState.Loading)
        yield(empty)
        yield(populated.copy(accounts = accounts.take(1)))
        yield(populated)
        yield(populated.copy(accounts = accounts.map { item ->
            if (item.account.id == 2L) item.copy(account = item.account.copy(currency = "USD")) else item
        }))
        yield(populated.copy(amountsAvailable = false))
        yield(populated.copy(accounts = listOf(accounts.first().let {
            it.copy(account = it.account.copy(name = "较长的账户名称用于核对卡面标题的截断与对齐"))
        })))
    }
}

@Preview(name = "Wallet Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Wallet Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WalletPreview(@PreviewParameter(WalletPreviewProvider::class) state: WalletUiState) {
    LedgerTheme { WalletContent(state, {}, {}) }
}
