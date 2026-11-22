package cn.turingzy.ledger.ui.feature.analytics.calculation

import cn.turingzy.ledger.ui.component.picker.CalendarSelection
import java.time.Month

/**
 * 获取适用于统计分析页面顶部展示的日期周期文本。
 *
 * 若选择范围覆盖完整的整年（1月1日到12月31日），单年返回如 "2026"，跨年返回如 "2025–2026"；
 * 其他粒度则回退使用 [CalendarSelection.displayRange]。
 *
 * @receiver 日期选择范围。
 * @return 格式化后的统计周期显示文案。
 */
internal fun CalendarSelection.analyticsDisplayRange(): String {
    val wholeYears = start.month == Month.JANUARY && start.dayOfMonth == 1 &&
        end.month == Month.DECEMBER && end.dayOfMonth == 31
    if (!wholeYears) return displayRange()
    return if (start.year == end.year) "${start.year}" else "${start.year}–${end.year}"
}
