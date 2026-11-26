package cn.turingzy.ledger.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.abs

/**
 * 金额格式化与解析工具类。
 *
 * 统一处理分（Long cents）与标准金额字符串之间的转换，包括货币符号拼接、负号位置处理、
 * 千分位千位分隔符展示以及文本输入反向解析为分单位。
 */
object AmountFormatter {

    /** 千分位及保留两位小数的格式化器 */
    private val decimalFormat = DecimalFormat("#,##0.00")

    /**
     * 将以分（Cents）为单位的长整型金额格式化为标准的金额字符串（保留两位小数）。
     *
     * @param cents 以分为单位的金额数值，支持正负数。
     * @param showSymbol 是否在金额前拼接货币符号，默认为 `false`。
     * @param symbol 货币符号字符串，默认为 "¥"。
     * @param useGrouping 是否使用千分位逗号分组（例如 "1,234.56"），默认为 `false`。
     * @return 格式化后的金额字符串。负数时符号置于负号之后（如 "-¥100.00"）。
     */
    fun formatCents(
        cents: Long,
        showSymbol: Boolean = false,
        symbol: String = "¥",
        useGrouping: Boolean = false,
    ): String {
        val isNegative = cents < 0
        // Divide before taking the absolute value so Long.MIN_VALUE remains representable.
        val numberString = if (useGrouping) {
            decimalFormat.format(BigDecimal.valueOf(cents, 2).abs())
        } else {
            val yuan = abs(cents / 100)
            val remainder = abs(cents % 100)
            "$yuan.${remainder.toString().padStart(2, '0')}"
        }

        val prefix = when {
            isNegative && showSymbol -> "-$symbol"
            isNegative -> "-"
            showSymbol -> symbol
            else -> ""
        }

        return "$prefix$numberString"
    }

    /**
     * 将用户输入的金额字符串解析为以分（Cents）为单位的长整型数值。
     *
     * 自动去除常见货币符号（"¥"、"$"）和千分位逗号，使用四舍五入保留两位小数并转为分。
     *
     * @param amountStr 待解析的金额字符串。
     * @return 对应的以分为单位的 [Long] 值；若解析失败或字符串为空则返回 `null`。
     */
    fun parseToCents(amountStr: String): Long? {
        val cleanStr = amountStr.trim().removePrefix("¥").removePrefix("$").replace(",", "").trim()
        if (cleanStr.isEmpty()) return null

        return runCatching {
            val bd = BigDecimal(cleanStr).setScale(2, RoundingMode.HALF_UP)
            bd.movePointRight(2).longValueExact()
        }.getOrNull()
    }
}

/**
 * 将以分（Cents）为单位的 [Long] 数值格式化为标准金额字符串的扩展函数。
 *
 * 底层调用 [AmountFormatter.formatCents]。
 *
 * @param showSymbol 是否在金额前拼接货币符号，默认为 `false`。
 * @param symbol 货币符号字符串，默认为 "¥"。
 * @param useGrouping 是否使用千分位逗号分组，默认为 `false`。
 * @return 格式化后的金额字符串。
 */
fun Long.toFormattedAmount(
    showSymbol: Boolean = false,
    symbol: String = "¥",
    useGrouping: Boolean = false,
): String = AmountFormatter.formatCents(
    cents = this,
    showSymbol = showSymbol,
    symbol = symbol,
    useGrouping = useGrouping,
)

