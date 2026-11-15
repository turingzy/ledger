package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.AccountType

internal fun AccountType.displayName(resources: android.content.res.Resources? = null): String = when (this) {
    AccountType.DEBIT -> resources?.getString(cn.turingzy.ledger.R.string.ui_debit_card) ?: "储蓄卡"
    AccountType.CREDIT -> resources?.getString(cn.turingzy.ledger.R.string.ui_credit_card) ?: "信用卡"
    AccountType.WALLET -> resources?.getString(cn.turingzy.ledger.R.string.ui_digital_wallet) ?: "电子钱包"
    AccountType.CASH -> resources?.getString(cn.turingzy.ledger.R.string.ui_cash) ?: "现金"
}
