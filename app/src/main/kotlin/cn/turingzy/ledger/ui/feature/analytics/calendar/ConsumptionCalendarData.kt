package cn.turingzy.ledger.ui.feature.analytics.calendar

import cn.turingzy.ledger.ui.component.picker.sundayFirstColumn
import cn.turingzy.ledger.ui.feature.analytics.calculation.AnalyticsDay
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth

/**
 * 消费日历展示所需的数据模型。
 *
 * @property cells 月历网格的单元格日期列表（包含周日首列对齐产生的占位 null）。
 * @property expenses 日期到当日支出金额（分）的映射（仅包含有支出的日期）。
 * @property levels 日期到支出热力等级（1 到 4 级）的映射。
 */
internal data class ConsumptionCalendarData(
    val cells: List<LocalDate?>,
    val expenses: Map<LocalDate, Long>,
    val levels: Map<LocalDate, Int>,
) {
    /** 当月总支出（分）。 */
    val totalExpense: Long get() = expenses.values.sum()

    /** 当月有消费支出的天数。 */
    val spendingDays: Int get() = expenses.size

    /** 当月单日最高支出金额（分）。 */
    val highestExpense: Long get() = expenses.values.maxOrNull() ?: 0L
}

/**
 * 聚合指定月份的消费日历展示数据。
 *
 * 计算该月份在以周日为首列的日历网格布局，筛选当月截止到今天的支出记录，
 * 并以当月最高单日支出为基准，将支出金额映射为 1~4 级热力色阶。
 *
 * @param days 全量按日统计数据映射。
 * @param month 目标年月。
 * @param today 今日日期。
 * @return 构造完成的 [ConsumptionCalendarData]。
 */
internal fun consumptionCalendarData(days: Map<LocalDate, AnalyticsDay>, month: YearMonth, today: LocalDate): ConsumptionCalendarData {
    val expenses = days.filterKeys { YearMonth.from(it) == month && it <= today }
        .mapValues { it.value.expense }.filterValues { it > 0L }
    val maximum = expenses.values.maxOrNull() ?: 0L
    val levels = expenses.mapValues { (_, amount) ->
        BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(4))
            .divide(BigDecimal.valueOf(maximum), 0, RoundingMode.CEILING).intValueExact().coerceIn(1, 4)
    }
    val leading = month.atDay(1).dayOfWeek.sundayFirstColumn()
    val count = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    val cells = List(count) { index ->
        val day = index - leading + 1
        if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }
    return ConsumptionCalendarData(cells, expenses, levels)
}
