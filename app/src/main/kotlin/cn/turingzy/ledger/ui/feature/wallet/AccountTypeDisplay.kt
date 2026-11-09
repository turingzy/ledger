package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.AccountType

internal fun AccountType.displayName(): String = when (this) {
    AccountType.DEBIT -> "储蓄卡"
    AccountType.CREDIT -> "信用卡"
    AccountType.WALLET -> "电子钱包"
    AccountType.CASH -> "现金"
}
