package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.primaryAmount
import cn.turingzy.ledger.model.primaryBalance
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

internal fun walletCurrencyTotals(accounts: List<AccountWithTemplate>, transactions: List<TransactionDetail>, primary: String = "CNY"): List<WalletCurrencyTotals> {
    val liveAccounts = accounts.map { it.account }.filterNot { it.isDeleted }
    val liveTransactions = transactions.filterNot { it.transaction.isDeleted }
    return listOf(WalletCurrencyTotals(
        currency = primary,
        assets = liveAccounts.filter { it.includeInTotal }.fold(0L) { total, account -> Math.addExact(total, account.primaryBalance(primary) ?: 0L) },
        income = liveTransactions.filter { it.transaction.type == TransactionType.INCOME }.fold(0L) { total, entry -> Math.addExact(total, entry.primaryAmount(primary) ?: 0L) },
        expense = liveTransactions.filter { it.transaction.type == TransactionType.EXPENSE }.fold(0L) { total, entry -> Math.addExact(total, entry.primaryAmount(primary) ?: 0L) },
        creditBalance = liveAccounts.filter { it.type == AccountType.CREDIT }.fold(0L) { total, account -> Math.addExact(total, account.primaryBalance(primary) ?: 0L) },
    ))
}
