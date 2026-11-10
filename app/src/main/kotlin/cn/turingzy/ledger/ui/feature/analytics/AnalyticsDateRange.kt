package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.component.calendar.CalendarSelection
import java.time.Month

internal fun CalendarSelection.analyticsDisplayRange(): String {
    val wholeYears = start.month == Month.JANUARY && start.dayOfMonth == 1 &&
        end.month == Month.DECEMBER && end.dayOfMonth == 31
    if (!wholeYears) return displayRange()
    return if (start.year == end.year) "${start.year}" else "${start.year}–${end.year}"
}
