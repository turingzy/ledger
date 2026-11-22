package cn.turingzy.ledger.ui.component.picker

import cn.turingzy.ledger.R
import java.time.DayOfWeek

/**
 * 月份日历网格周标题字符串资源列表（以星期日作为每周起始列）。
 */
internal val MonthGridWeekdays = listOf(R.string.ui_sun_2, R.string.ui_mon, R.string.ui_tue,
    R.string.ui_wed, R.string.ui_thu_2, R.string.ui_fri, R.string.ui_sat)

/**
 * 获取星期几在以星期日为第一列（0-indexed）的日历网格中的列索引。
 *
 * @return 0（周日）至 6（周六）的列索引。
 */
internal fun DayOfWeek.sundayFirstColumn(): Int = value % 7
