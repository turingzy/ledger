package cn.turingzy.ledger.ui.feature.bill.search

import cn.turingzy.ledger.model.currency.primaryAmount
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.ui.feature.bill.DayTransactionGroup
import cn.turingzy.ledger.ui.feature.bill.groupTransactionsByDay
import java.io.Serializable
import java.time.LocalDate
import java.time.ZoneId

/**
 * 账单搜索结果的排序方式枚举。
 */
internal enum class BillSearchSort {
    /** 默认排序（按时间倒序分组）。 */
    DEFAULT,
    /** 按主币种金额升序排列。 */
    AMOUNT_ASC,
    /** 按主币种金额降序排列。 */
    AMOUNT_DESC,
    /** 按交易时间升序排列。 */
    TIME_ASC,
    /** 按交易时间降序排列。 */
    TIME_DESC;

    /** 是否属于按金额维度的排序。 */
    val isAmountSort: Boolean get() = this == AMOUNT_ASC || this == AMOUNT_DESC
}

/**
 * 账单搜索高级过滤区间（日期和金额闭区间，金额单位为分）。
 *
 * @property start 过滤起始日期（包含）。
 * @property end 过滤结束日期（包含）。
 * @property minimum 最低交易金额（主币种分，包含）。
 * @property maximum 最高交易金额（主币种分，包含）。
 */
internal data class BillSearchFilters(
    val start: LocalDate? = null,
    val end: LocalDate? = null,
    val minimum: Long? = null,
    val maximum: Long? = null,
) : Serializable

/**
 * 账单搜索请求参数。
 *
 * @property query 文本搜索关键词（匹配备注或分类名）。
 * @property categoryId 指定筛选的分类 ID。
 * @property filters 自定义起止日期及金额区间过滤条件。
 * @property sort 排序规则 [BillSearchSort]。
 * @property accountId 指定筛选的账户 ID（包括转出或转入账户）。
 * @property datePreset 预设日期快捷选项。
 * @property amountPreset 预设金额快捷选项。
 */
internal data class BillSearchRequest(
    val query: String = "",
    val categoryId: Long? = null,
    val filters: BillSearchFilters = BillSearchFilters(),
    val sort: BillSearchSort = BillSearchSort.DEFAULT,
    val accountId: Long? = null,
    val datePreset: SearchDatePreset? = SearchDatePreset.ALL,
    val amountPreset: SearchAmountPreset? = SearchAmountPreset.ALL,
) : Serializable {
    /** 是否指定了明确的搜索目标（关键词或分类）。 */
    val hasSearchTarget: Boolean get() = query.isNotBlank() || categoryId != null
}

/**
 * 账单搜索结果数据载体。
 *
 * @property currency 计算和展示金额所用的主币种。
 * @property groups 按天聚合后的交易分组列表（时间排序模式下生效）。
 * @property ungrouped 未按天聚合的交易平面列表（按金额全局排序时生效）。
 */
internal data class BillSearchResults(
    val currency: String,
    val groups: List<DayTransactionGroup> = emptyList(),
    val ungrouped: List<TransactionDetail> = emptyList(),
)

/**
 * 根据搜索请求过滤交易记录列表，并根据排序类型构建分组或全局扁平结果。
 *
 * 当按时间排序时，保持按天分组结构；当按金额排序时，采用全局扁平排序并将无折算汇率的项置于末尾。
 *
 * @param rows 待匹配的候选交易详情列表。
 * @param request 搜索请求条件 [BillSearchRequest]。
 * @param currency 折算和比对金额的主币种代码。
 * @param zone 判定日期区间的时区。
 * @return 构建完成的 [BillSearchResults]。
 */
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
