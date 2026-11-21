package cn.turingzy.ledger.ui.component.picker

import cn.turingzy.ledger.R
import java.time.DayOfWeek

/** Labels and column indices for month grids with Sunday in the first column. */
internal val MonthGridWeekdays = listOf(R.string.ui_sun_2, R.string.ui_mon, R.string.ui_tue,
    R.string.ui_wed, R.string.ui_thu_2, R.string.ui_fri, R.string.ui_sat)

internal fun DayOfWeek.sundayFirstColumn(): Int = value % 7
