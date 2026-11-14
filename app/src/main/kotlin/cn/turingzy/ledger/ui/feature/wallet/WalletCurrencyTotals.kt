package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType

data class WalletCurrencyTotals(
    val currency: String,
    val assets: Long = 0L,
    val income: Long = 0L,
    val expense: Long = 0L,
    val creditBalance: Long = 0L,
)

internal fun walletCurrencyTotals(accounts: List<AccountWithTemplate>, transactions: List<TransactionDetail>): List<WalletCurrencyTotals> {
    val liveAccounts = accounts.map { it.account }.filterNot { it.isDeleted }
    val liveTransactions = transactions.filterNot { it.transaction.isDeleted }
    val currencies = (liveAccounts.map { it.currency } + liveTransactions.map { it.account.currency }).distinct()
        .sortedWith(compareBy<String> { it != "CNY" }.thenBy { it })
    return currencies.ifEmpty { listOf("CNY") }.map { currency ->
        val currencyAccounts = liveAccounts.filter { it.currency == currency }
        val currencyTransactions = liveTransactions.filter { it.account.currency == currency }
        WalletCurrencyTotals(
            currency = currency,
            assets = currencyAccounts.filter { it.includeInTotal }.fold(0L) { total, account -> Math.addExact(total, account.currentBalance) },
            income = currencyTransactions.filter { it.transaction.type == TransactionType.INCOME }.fold(0L) { total, entry -> Math.addExact(total, entry.transaction.amount) },
            expense = currencyTransactions.filter { it.transaction.type == TransactionType.EXPENSE }.fold(0L) { total, entry -> Math.addExact(total, entry.transaction.amount) },
            creditBalance = currencyAccounts.filter { it.type == AccountType.CREDIT }.fold(0L) { total, account -> Math.addExact(total, account.currentBalance) },
        )
    }
}
