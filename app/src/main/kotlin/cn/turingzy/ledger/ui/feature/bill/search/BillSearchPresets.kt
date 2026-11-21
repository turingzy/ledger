package cn.turingzy.ledger.ui.feature.bill.search

import androidx.annotation.StringRes
import cn.turingzy.ledger.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters.previousOrSame

/** Date presets use the same Monday-through-Sunday week as the bill page. */
internal enum class SearchDatePreset(@param:StringRes val label: Int) {
    ALL(R.string.ui_all),
    THIS_WEEK(R.string.bill_search_this_week),
    THIS_MONTH(R.string.bill_search_this_month),
    LAST_MONTH(R.string.bill_search_last_month),
    LAST_THREE_MONTHS(R.string.bill_search_last_three_months),
    THIS_YEAR(R.string.bill_search_this_year),
    LAST_YEAR(R.string.bill_search_last_year);

    fun applyTo(filters: BillSearchFilters, today: LocalDate): BillSearchFilters = when (this) {
        ALL -> filters.copy(start = null, end = null)
        THIS_WEEK -> {
            val start = today.with(previousOrSame(DayOfWeek.MONDAY))
            filters.copy(start = start, end = start.plusDays(6))
        }
        THIS_MONTH, LAST_MONTH -> {
            val month = YearMonth.from(today).minusMonths(if (this == LAST_MONTH) 1 else 0)
            filters.copy(start = month.atDay(1), end = month.atEndOfMonth())
        }
        LAST_THREE_MONTHS -> filters.copy(start = today.minusMonths(3), end = today)
        THIS_YEAR, LAST_YEAR -> {
            val year = today.year - if (this == LAST_YEAR) 1 else 0
            filters.copy(start = LocalDate.of(year, 1, 1), end = LocalDate.of(year, 12, 31))
        }
    }
}

/** Adjacent presets use nonoverlapping, inclusive cent bounds. */
internal enum class SearchAmountPreset(@param:StringRes val label: Int, val minimum: Long?, val maximum: Long?) {
    ALL(R.string.ui_all, null, null),
    UNDER_100(R.string.bill_search_under_100, null, 9_999),
    FROM_100(R.string.bill_search_100_to_1k, 10_000, 99_999),
    FROM_1K(R.string.bill_search_1k_to_10k, 100_000, 999_999),
    FROM_10K(R.string.bill_search_10k_to_100k, 1_000_000, 9_999_999),
    AT_LEAST_100K(R.string.bill_search_100k_and_up, 10_000_000, null);

    fun applyTo(filters: BillSearchFilters): BillSearchFilters = filters.copy(minimum = minimum, maximum = maximum)
}
