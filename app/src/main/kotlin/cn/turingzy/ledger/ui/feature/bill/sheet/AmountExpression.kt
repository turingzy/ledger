package cn.turingzy.ledger.ui.feature.bill.sheet

import cn.turingzy.ledger.util.AmountFormatter
import cn.turingzy.ledger.util.toFormattedAmount
import java.math.BigDecimal
import java.math.RoundingMode

private const val RECORD_OPERATORS = "+-×÷"
private val recordNumber = Regex("[+-]?[0-9]+(?:\\.[0-9]{0,2})?")

/**
 * 计算记账金额四则运算表达式的结果。
 *
 * 支持加（+）、减（-）、乘（× 或 *）、除（÷ 或 /），并按标准优先级（先乘除后加减）以精确金额（分）进行运算。
 * 若表达式不完整（如以运算符结尾），会先去除末尾运算符再行计算；若存在非法字符或除以零则返回空字符串。
 *
 * @param expr 待求值的数学表达式字符串。
 * @return 运算后的格式化金额字符串（如 "12.5" 或 "100"），计算失败时返回空字符串。
 */
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

/**
 * 向记账金额表达式追加运算符。
 *
 * 处理连击转换逻辑：连续输入两个 `+` 会自动升级为乘号 `×`；连续输入两个 `-` 会升级为除号 `÷`；
 * 若末尾已存在其他运算符，则替换为新的运算符。
 *
 * @param expression 当前输入的表达式。
 * @param operator 待追加的基础运算符（"+" 或 "-"）。
 * @return 追加或转换后的新表达式。
 */
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

/**
 * 提取表达式中的最后一个操作数。
 *
 * @param expression 当前运算表达式。
 * @return 最右侧未闭合的数字操作数字符串。
 */
internal fun recordLastOperand(expression: String): String = expression.split('+', '-', '×', '÷').last()
