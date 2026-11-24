package cn.turingzy.ledger.ui.feature.bill.search

import androidx.annotation.StringRes
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.picker.CalendarSelection
import java.time.LocalDate
import java.time.YearMonth

/**
 * 账单搜索常用的预设日期区间。
 *
 * @property label 对应的字符串资源 ID。
 */
internal enum class SearchDatePreset(@param:StringRes val label: Int) {
    /** 全部日期。 */
    ALL(R.string.ui_all),
    /** 本周（周日至周六）。 */
    THIS_WEEK(R.string.bill_search_this_week),
    /** 本月。 */
    THIS_MONTH(R.string.bill_search_this_month),
    /** 上月。 */
    LAST_MONTH(R.string.bill_search_last_month),
    /** 近三月。 */
    LAST_THREE_MONTHS(R.string.bill_search_last_three_months),
    /** 今年。 */
    THIS_YEAR(R.string.bill_search_this_year),
    /** 去年。 */
    LAST_YEAR(R.string.bill_search_last_year);

    /**
     * 将预设换算为具体起止日期并更新到 [BillSearchFilters] 中。
     *
     * @param filters 原筛选配置。
     * @param today 当前基准日期。
     * @return 更新了起止日期的 [BillSearchFilters]。
     */
    fun applyTo(filters: BillSearchFilters, today: LocalDate): BillSearchFilters = when (this) {
        ALL -> filters.copy(start = null, end = null)
        THIS_WEEK -> {
            val (start, end) = CalendarSelection.week(today)
            filters.copy(start = start, end = end)
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

/**
 * 账单搜索常用的预设金额区间（分）。
 *
 * @property label 对应的字符串资源 ID。
 * @property minimum 最低金额（分），为空表示无下限。
 * @property maximum 最高金额（分），为空表示无上限。
 */
internal enum class SearchAmountPreset(@param:StringRes val label: Int, val minimum: Long?, val maximum: Long?) {
    /** 全部金额。 */
    ALL(R.string.ui_all, null, null),
    /** 100 元以内（< 10000 分）。 */
    UNDER_100(R.string.bill_search_under_100, null, 9_999),
    /** 100 - 1000 元（10000 - 99999 分）。 */
    FROM_100(R.string.bill_search_100_to_1k, 10_000, 99_999),
    /** 1000 - 10000 元（100000 - 999999 分）。 */
    FROM_1K(R.string.bill_search_1k_to_10k, 100_000, 999_999),
    /** 1万 - 10万 元（1000000 - 9999999 分）。 */
    FROM_10K(R.string.bill_search_10k_to_100k, 1_000_000, 9_999_999),
    /** 10万 元以上（>= 10000000 分）。 */
    AT_LEAST_100K(R.string.bill_search_100k_and_up, 10_000_000, null);

    /**
     * 将当前预设金额区间写入到 [BillSearchFilters] 中。
     *
     * @param filters 原筛选配置。
     * @return 更新了金额区间的 [BillSearchFilters]。
     */
    fun applyTo(filters: BillSearchFilters): BillSearchFilters = filters.copy(minimum = minimum, maximum = maximum)
}
