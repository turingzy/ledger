package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.model.currency.primaryBalance
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType

/**
 * 钱包币种统计汇总数据类。
 *
 * @property currency 币种代码（如 "CNY"）
 * @property assets 计入总资产的账户折算总额（单位：分）
 * @property income 历史未删除收入交易折算总额（单位：分）
 * @property expense 历史未删除支出交易折算总额（单位：分）
 * @property creditBalance 信用账户折算总额（单位：分）
 */
data class WalletCurrencyTotals(
    val currency: String,
    val assets: Long = 0L,
    val income: Long = 0L,
    val expense: Long = 0L,
    val creditBalance: Long = 0L,
)

/**
 * 根据未删除的账户列表与交易列表，计算指定主币种下的钱包资产与收支统计数据。
 *
 * @param accounts 账户及其模板列表
 * @param transactions 交易明细列表
 * @param primary 主币种代码，默认为 "CNY"
 * @return 包含指定主币种统计汇总信息的列表
 */
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
