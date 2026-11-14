package cn.turingzy.ledger.ui.feature.analytics

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

internal data class ConsumptionCalendarData(
    val cells: List<LocalDate?>,
    val expenses: Map<LocalDate, Long>,
    val levels: Map<LocalDate, Int>,
) {
    val totalExpense: Long get() = expenses.values.sum()
    val spendingDays: Int get() = expenses.size
    val highestExpense: Long get() = expenses.values.maxOrNull() ?: 0L
}

internal fun consumptionCalendarData(days: Map<LocalDate, AnalyticsDay>, month: YearMonth, today: LocalDate): ConsumptionCalendarData {
    val expenses = days.filterKeys { YearMonth.from(it) == month && it <= today }
        .mapValues { it.value.expense }.filterValues { it > 0L }
    val maximum = expenses.values.maxOrNull() ?: 0L
    val levels = expenses.mapValues { (_, amount) ->
        BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(4))
            .divide(BigDecimal.valueOf(maximum), 0, RoundingMode.CEILING).intValueExact().coerceIn(1, 4)
    }
    val leading = month.atDay(1).dayOfWeek.value - 1
    val count = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    val cells = List(count) { index ->
        val day = index - leading + 1
        if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }
    return ConsumptionCalendarData(cells, expenses, levels)
}
