package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.util.toFormattedAmount

fun evaluateMathExpression(expr: String): String {
    val expression = expr.trim().trimEnd('+', '-')
    if (expression.isEmpty()) return ""
    val tokens = Regex("([+-]?)([0-9]+)(?:\\.([0-9]{0,2}))?").findAll(expression)
    var consumed = 0
    var total = 0L
    for (token in tokens) {
        if (token.range.first != consumed) return ""
        if (consumed > 0 && token.groupValues[1].isEmpty()) return ""
        consumed = token.range.last + 1
        val whole = token.groupValues[2].toLongOrNull() ?: return ""
        val fraction = token.groupValues[3].padEnd(2, '0').toLongOrNull() ?: return ""
        if (whole > (Long.MAX_VALUE - fraction) / 100L) return ""
        val cents = whole * 100L + fraction
        val signedCents = if (token.groupValues[1] == "-") -cents else cents
        // 保留对称金额范围，让负数取绝对值后的展示也不会溢出。
        if (signedCents > 0L && total > Long.MAX_VALUE - signedCents) return ""
        if (signedCents < 0L && total < -Long.MAX_VALUE - signedCents) return ""
        total += signedCents
    }
    if (consumed != expression.length) return ""
    return total.toFormattedAmount().trimEnd('0').trimEnd('.')
}
