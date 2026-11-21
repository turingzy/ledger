package cn.turingzy.ledger.ui.feature.bill.search

import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.ui.feature.bill.DayTransactionGroup
import cn.turingzy.ledger.ui.feature.bill.groupTransactionsByDay
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId

internal enum class BillSearchSort {
    DEFAULT, AMOUNT_ASC, AMOUNT_DESC, TIME_ASC, TIME_DESC;

    val isAmountSort: Boolean get() = this == AMOUNT_ASC || this == AMOUNT_DESC
}

/** The optional bounds are inclusive and amounts are in primary-currency cents. */
internal data class BillSearchFilters(
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val minimum: Long? = null,
    val maximum: Long? = null,
) : Serializable {
    val active: Boolean get() = start != null || end != null || minimum != null || maximum != null
}

internal data class BillSearchRequest(
    val query: String = "",
    val categoryId: Long? = null,
    val filters: BillSearchFilters = BillSearchFilters(),
    val sort: BillSearchSort = BillSearchSort.DEFAULT,
    val accountId: Long? = null,
    val datePreset: SearchDatePreset? = SearchDatePreset.ALL,
    val amountPreset: SearchAmountPreset? = SearchAmountPreset.ALL,
) : Serializable {
    val active: Boolean get() = query.isNotBlank() || categoryId != null || filters.active ||
        sort != BillSearchSort.DEFAULT || accountId != null
}

internal data class BillSearchResults(
    val currency: String,
    val groups: List<DayTransactionGroup> = emptyList(),
    val ungrouped: List<TransactionDetail> = emptyList(),
)

/** Keeps amount ordering global and puts rows without a conversion at the end. */
internal fun buildBillSearchResults(
    rows: List<TransactionDetail>,
    request: BillSearchRequest,
    currency: String,
    zone: ZoneId,
): BillSearchResults {
    val filters = request.filters
    val firstMillis = filters.start?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
    val afterLastMillis = filters.end?.plusDays(1)?.atStartOfDay(zone)?.toInstant()?.toEpochMilli()
    val matching = rows.filter { detail ->
        val transaction = detail.transaction
        if (firstMillis != null && transaction.transactionTime < firstMillis) return@filter false
        if (afterLastMillis != null && transaction.transactionTime >= afterLastMillis) return@filter false
        if (request.accountId != null && request.accountId != transaction.accountId &&
            request.accountId != transaction.toAccountId) return@filter false
        if (filters.minimum == null && filters.maximum == null) return@filter true
        val amount = detail.primaryAmount(currency) ?: return@filter false
        (filters.minimum == null || amount >= filters.minimum) &&
            (filters.maximum == null || amount <= filters.maximum)
    }

    if (!request.sort.isAmountSort) return BillSearchResults(
        currency, groups = groupTransactionsByDay(matching, zone, currency, request.sort != BillSearchSort.TIME_ASC),
    )
    val (valued, unvalued) = matching.partition { it.primaryAmount(currency) != null }
    val ordered = if (request.sort == BillSearchSort.AMOUNT_ASC) valued.sortedBy { it.primaryAmount(currency) }
        else valued.sortedByDescending { it.primaryAmount(currency) }
    return BillSearchResults(currency, ungrouped = ordered + unvalued)
}
