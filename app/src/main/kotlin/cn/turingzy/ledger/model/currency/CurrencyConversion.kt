package cn.turingzy.ledger.model.currency

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.transaction.TransactionDetail
import java.math.BigDecimal
import java.math.RoundingMode

fun convertCents(amount: Long, rate: String): Long {
    val multiplier = rate.toBigDecimal()
    require(multiplier.signum() > 0 && multiplier.scale() in -18..18 && multiplier.precision() <= 36) { "汇率必须大于零且小数不超过 18 位" }
    return BigDecimal.valueOf(amount).multiply(multiplier).setScale(0, RoundingMode.HALF_UP).longValueExact()
}

fun TransactionDetail.primaryAmount(currency: String): Long? = when {
    account.currency == currency -> transaction.amount
    transaction.baseCurrency == currency -> transaction.baseAmount
    else -> null
}

fun Account.primaryBalance(currency: String): Long? = when {
    this.currency == currency -> currentBalance
    baseCurrency == currency -> baseBalance
    else -> null
}
