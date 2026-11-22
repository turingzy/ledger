package cn.turingzy.ledger.model.currency

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.transaction.TransactionDetail
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 根据汇率换算金额（以分为单位）。
 *
 * 使用 [BigDecimal] 高精度乘法计算，采用四舍五入（[RoundingMode.HALF_UP]）保留至整数分。
 *
 * @param amount 原始金额（单位：分）
 * @param rate 汇率乘数字符串（必须大于零，且小数位在 -18 到 18 之间，有效位数不超过 36）
 * @return 换算后的金额（单位：分）
 * @throws IllegalArgumentException 当汇率非正或数值精度超出限制时抛出
 */
fun convertCents(amount: Long, rate: String): Long {
    val multiplier = rate.toBigDecimal()
    require(multiplier.signum() > 0 && multiplier.scale() in -18..18 && multiplier.precision() <= 36) { "汇率必须大于零且小数不超过 18 位" }
    return BigDecimal.valueOf(amount).multiply(multiplier).setScale(0, RoundingMode.HALF_UP).longValueExact()
}

/**
 * 币种金额手动覆盖数据模型。
 *
 * 在跨币种记账或转账时，支持用户直接指定目标币种的实际到账金额，并据此反推实际汇率。
 *
 * @property currency 目标币种代码
 * @property amount 目标币种实际金额（单位：分）
 */
data class CurrencyAmountOverride(val currency: String, val amount: Long) {
    /**
     * 根据原始币种金额反推并校验汇率。
     *
     * 计算 `amount / originalAmount` 保留 18 位小数，并验证该汇率经 [convertCents] 换算后能完全吻合目标金额。
     *
     * @param originalAmount 原始币种金额（单位：分，必须大于 0）
     * @return 消除末尾无效 0 后的汇率普通字符串形式
     * @throws IllegalArgumentException 当金额非正或换算校验不一致时抛出
     */
    fun rateFor(originalAmount: Long): String {
        require(originalAmount > 0L && amount > 0L)
        val rate = BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(originalAmount), 18, RoundingMode.HALF_UP)
            .stripTrailingZeros().toPlainString()
        require(convertCents(originalAmount, rate) == amount)
        return rate
    }
}

/**
 * 获取交易在指定主币种下的金额。
 *
 * @param currency 期望的目标主币种代码
 * @return 折合主币种后的金额（单位：分）；若币种不匹配且未记录基准折算金额则返回 null
 */
fun TransactionDetail.primaryAmount(currency: String): Long? = when {
    account.currency == currency -> transaction.amount
    transaction.baseCurrency == currency -> transaction.baseAmount
    else -> null
}

/**
 * 获取账户在指定主币种下的余额。
 *
 * @param currency 期望的目标主币种代码
 * @return 折合主币种后的余额（单位：分）；若币种不匹配且无折算基准余额则返回 null
 */
fun Account.primaryBalance(currency: String): Long? = when {
    this.currency == currency -> currentBalance
    baseCurrency == currency -> baseBalance
    else -> null
}
