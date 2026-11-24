package cn.turingzy.ledger.ui.component.picker

import cn.turingzy.ledger.model.transaction.DateFilterType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters.previousOrSame

/**
 * 获取指定日期所在周的周日日期。
 *
 * @param date 目标日期。
 * @return 所在周周日的 [LocalDate]。
 */
internal fun weekStart(date: LocalDate): LocalDate = date.with(previousOrSame(DayOfWeek.SUNDAY))

/**
 * 获取指定日期所在周的文本显示范围（如 "2024 05.05-05.11"）。
 *
 * @param date 目标日期。
 * @return 格式化后的周范围文本。
 */
internal fun weekLabel(date: LocalDate): String = CalendarSelection.week(date).displayRange()

/**
 * 根据周期筛选类型获取包含该日期的周期起始日期。
 *
 * - 周视图：该周周日
 * - 月视图：该月1日
 * - 年视图：该年1月1日
 * - 自定义范围：原始日期
 *
 * @param type 周期筛选类型 [DateFilterType]。
 * @param date 目标日期。
 * @return 对应周期的起始 [LocalDate]。
 */
internal fun periodStart(type: DateFilterType, date: LocalDate): LocalDate = when (type) {
    DateFilterType.WEEK -> weekStart(date)
    DateFilterType.MONTH -> date.withDayOfMonth(1)
    DateFilterType.YEAR -> date.withDayOfYear(1)
    DateFilterType.RANGE -> date
}

/**
 * 格式化输出指定周期类型在目标日期下的文本标签。
 *
 * @param type 周期类型。
 * @param date 目标日期。
 * @return 周期标签（如 "2024.05" 或 "2024"）。
 */
internal fun periodLabel(type: DateFilterType, date: LocalDate): String = when (type) {
    DateFilterType.WEEK -> weekLabel(date)
    DateFilterType.MONTH -> date.format(DateTimeFormatter.ofPattern("yyyy.MM"))
    DateFilterType.YEAR -> date.year.toString()
    DateFilterType.RANGE -> date.toString()
}

/**
 * 当前周期类型对应的本期文案描述（"本周"、"本月"、"本年"）。
 */
internal val DateFilterType.currentPeriodLabel: String
    get() = when (this) {
        DateFilterType.WEEK -> "本周"
        DateFilterType.MONTH -> "本月"
        DateFilterType.YEAR -> "本年"
        DateFilterType.RANGE -> "本月"
    }

/**
 * 快速跳转回当前周期的操作按钮文案（如 "返回本月"）。
 */
internal val DateFilterType.returnLabel: String
    get() = "返回$currentPeriodLabel"

/**
 * 周期序列生成与索引计算辅助类。
 *
 * 用于在周/月/年滚动列表中按固定时间单位生成从起始年份（最低 1900）到结束年份（最高 2100）的连续时间序列。
 *
 * @param type 周期类型（不能为 [DateFilterType.RANGE]）。
 * @param reference 基准参考日期，用于确保序列覆盖该年份。
 */
internal class PeriodSeries(private val type: DateFilterType, reference: LocalDate) {
    init { require(type != DateFilterType.RANGE) }

    private val first = periodStart(type, LocalDate.of(minOf(1900, reference.year), 1, 1))
    private val last = periodStart(type, LocalDate.of(maxOf(2100, reference.year), 12, 31))
    private val unit = when (type) {
        DateFilterType.WEEK -> ChronoUnit.WEEKS
        DateFilterType.MONTH -> ChronoUnit.MONTHS
        DateFilterType.YEAR -> ChronoUnit.YEARS
        DateFilterType.RANGE -> error("Range is not a scrollable period")
    }

    /**
     * 序列中包含的周期单元总数量。
     */
    val size: Int = Math.toIntExact(unit.between(first, last) + 1)

    /**
     * 获取指定索引位置对应的周期起始日期。
     *
     * @param index 序列索引（从 0 到 [size] - 1）。
     * @return 对应的周期起始 [LocalDate]。
     */
    fun dateAt(index: Int): LocalDate {
        require(index in 0 until size)
        return first.plus(index.toLong(), unit)
    }

    /**
     * 计算给定日期在序列中所对应的周期索引位置。
     *
     * @param date 目标日期。
     * @return 限制在有效范围内的索引值。
     */
    fun indexOf(date: LocalDate): Int = unit.between(first, periodStart(type, date))
        .coerceIn(0L, size.toLong() - 1).toInt()
}
