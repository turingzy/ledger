package cn.turingzy.ledger.ui.component.picker

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * 日历日期区间选区模型。
 *
 * 表示一段由起止日期构成的连续时间范围，提供区间包含判定、端点拖拽调整及按年/月/周快速构建等功能。
 *
 * @property start 区间起始日期（闭区间）。
 * @property end 区间结束日期（闭区间），必须不早于 [start]。
 */
internal data class CalendarSelection(val start: LocalDate, val end: LocalDate) {
    init {
        require(!end.isBefore(start))
    }

    /**
     * 判断指定日期是否落在当前选区范围内。
     *
     * @param date 待检测的日期。
     * @return 若位于闭区间 [start, end] 内则返回 true，否则返回 false。
     */
    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    /**
     * 格式化输出用于界面展示的日期区间文本。
     *
     * 同年格式为 "yyyy MM.dd-MM.dd"，跨年格式为 "yyyy MM.dd-yyyy MM.dd"。
     */
    fun displayRange(separator: String = "-"): String {
        val startLabel = start.format(RANGE_START_FORMAT)
        val endLabel = end.format(if (start.year == end.year) RANGE_END_FORMAT else RANGE_START_FORMAT)
        return "$startLabel$separator$endLabel"
    }

    /**
     * 调整选区的起止端点。
     *
     * @param date 目标日期。
     * @param startHandle 是否调整起始把手（true 为调整起始端点，false 为调整结束端点）。
     * @param wholeMonths 是否对齐到整月（起始端点取月初，结束端点取月末）。
     * @return 调整后的新 [CalendarSelection]。
     */
    fun resize(date: LocalDate, startHandle: Boolean, wholeMonths: Boolean): CalendarSelection {
        val candidate = if (wholeMonths) {
            if (startHandle) date.withDayOfMonth(1) else YearMonth.from(date).atEndOfMonth()
        } else {
            date
        }
        return if (startHandle) {
            val boundary = if (wholeMonths) YearMonth.from(end).atEndOfMonth() else end
            CalendarSelection(minOf(candidate, if (wholeMonths) boundary.withDayOfMonth(1) else boundary), boundary)
        } else {
            val boundary = if (wholeMonths) start.withDayOfMonth(1) else start
            CalendarSelection(boundary, maxOf(candidate, if (wholeMonths) YearMonth.from(boundary).atEndOfMonth() else boundary))
        }
    }

    /**
     * 按整年维度调整选区的起止年份。
     *
     * @param date 目标日期（提取其年份）。
     * @param startHandle 是否调整起始把手。
     * @return 调整后的整年区间（起始为对应年 1 月 1 日，结束为对应年 12 月 31 日）。
     */
    fun resizeYears(date: LocalDate, startHandle: Boolean): CalendarSelection {
        val firstYear = if (startHandle) minOf(date.year, end.year) else start.year
        val lastYear = if (startHandle) end.year else maxOf(date.year, start.year)
        return CalendarSelection(LocalDate.of(firstYear, 1, 1), LocalDate.of(lastYear, 12, 31))
    }

    companion object {
        private val RANGE_START_FORMAT = DateTimeFormatter.ofPattern("yyyy MM.dd")
        private val RANGE_END_FORMAT = DateTimeFormatter.ofPattern("MM.dd")

        /**
         * 创建指定年份全年的选区（1月1日至12月31日）。
         *
         * @param year 年份。
         */
        fun year(year: Int): CalendarSelection =
            CalendarSelection(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))

        /**
         * 创建包含指定日期的完整周选区（周日至周六）。
         *
         * @param date 目标日期。
         */
        fun week(date: LocalDate): CalendarSelection {
            val start = weekStart(date)
            return CalendarSelection(start, start.plusDays(6))
        }

        /**
         * 创建指定月份全月的选区（当月1日至当月末日）。
         *
         * @param month 目标年月。
         */
        fun month(month: YearMonth): CalendarSelection = CalendarSelection(month.atDay(1), month.atEndOfMonth())
    }
}
