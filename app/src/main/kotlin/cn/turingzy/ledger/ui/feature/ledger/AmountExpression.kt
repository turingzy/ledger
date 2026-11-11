package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.util.AmountFormatter
import cn.turingzy.ledger.util.toFormattedAmount
import java.math.BigDecimal
import java.math.RoundingMode

private const val RECORD_OPERATORS = "+-×÷"
private val recordNumber = Regex("[+-]?[0-9]+(?:\\.[0-9]{0,2})?")

fun evaluateMathExpression(expr: String): String {
    val expression = expr.trim().replace('*', '×').replace('/', '÷').replace('−', '-')
        .trimEnd { it in RECORD_OPERATORS }
    if (expression.isEmpty()) return ""
    var position = 0
    fun nextAmount(): Long? {
        val match = recordNumber.find(expression, position)?.takeIf { it.range.first == position } ?: return null
        position = match.range.last + 1
        return AmountFormatter.parseToCents(match.value)?.takeUnless { it == Long.MIN_VALUE }
    }
    var term = nextAmount() ?: return ""
    var sum = 0L
    return try {
        while (position < expression.length) {
            val operator = expression[position++]
            if (operator !in RECORD_OPERATORS) return ""
            val amount = nextAmount() ?: return ""
            when (operator) {
                '+', '-' -> {
                    sum = Math.addExact(sum, term)
                    term = if (operator == '-') Math.negateExact(amount) else amount
                }
                '×' -> term = BigDecimal.valueOf(term).multiply(BigDecimal.valueOf(amount))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).longValueExact()
                '÷' -> {
                    if (amount == 0L) return ""
                    term = BigDecimal.valueOf(term).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(amount), 0, RoundingMode.HALF_UP).longValueExact()
                }
            }
            if (term == Long.MIN_VALUE) return ""
        }
        val total = Math.addExact(sum, term)
        if (total == Long.MIN_VALUE) "" else total.toFormattedAmount().trimEnd('0').trimEnd('.')
    } catch (_: ArithmeticException) {
        ""
    }
}

internal fun appendRecordOperator(expression: String, operator: String): String {
    if (expression.isEmpty() || expression.length >= 80) return expression
    val last = expression.last()
    val next = when (operator) {
        "+" -> if (last == '+') '×' else '+'
        "-" -> if (last == '-') '÷' else '-'
        else -> return expression
    }
    return if (last in RECORD_OPERATORS) expression.dropLast(1) + next else expression + next
}

internal fun recordLastOperand(expression: String): String = expression.split('+', '-', '×', '÷').last()
