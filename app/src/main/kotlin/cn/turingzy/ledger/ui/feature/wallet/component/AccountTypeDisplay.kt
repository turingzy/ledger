package cn.turingzy.ledger.ui.feature.wallet.component

import cn.turingzy.ledger.model.account.AccountType

/**
 * 获取账户类型的本地化显示名称。
 *
 * @param resources Android 资源对象，若为空则返回默认中文文案
 * @return 对应的账户类型名称（如"储蓄卡"、"信用卡"等）
 */
internal fun AccountType.displayName(resources: android.content.res.Resources? = null): String = when (this) {
    AccountType.DEBIT -> resources?.getString(cn.turingzy.ledger.R.string.ui_debit_card) ?: "储蓄卡"
    AccountType.CREDIT -> resources?.getString(cn.turingzy.ledger.R.string.ui_credit_card) ?: "信用卡"
    AccountType.WALLET -> resources?.getString(cn.turingzy.ledger.R.string.ui_digital_wallet) ?: "电子钱包"
    AccountType.CASH -> resources?.getString(cn.turingzy.ledger.R.string.ui_cash) ?: "现金"
}
