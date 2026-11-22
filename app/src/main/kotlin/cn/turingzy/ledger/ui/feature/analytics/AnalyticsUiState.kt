package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.model.category.CategoryType
import java.time.LocalDate

/**
 * 统计分析的时间跨度粒度。
 */
enum class AnalyticsTimeScope {
    /** 按周统计。 */
    WEEK,
    /** 按月统计。 */
    MONTH,
    /** 按季度（三月）统计。 */
    THREE_MONTHS,
    /** 按年统计。 */
    YEAR,
    /** 全部/自定义区间统计。 */
    ALL,
}

/**
 * 分类收支排行榜数据项。
 *
 * @property categoryId 分类 ID。
 * @property categoryName 分类名称。
 * @property totalAmount 当前分类在所选周期的总金额（分）。
 * @property percentage 在总收支中的占比（0f ~ 1f）。
 * @property transactions 该分类下的交易列表。
 */
data class CategoryExpenseRanking(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: Long,
    val percentage: Float,
    val transactions: List<AnalyticsTransaction> = emptyList(),
)

/**
 * 统计分析模块中的账单明细数据模型。
 *
 * @property id 账单交易 ID。
 * @property date 交易日期。
 * @property note 交易备注。
 * @property amount 原始交易金额（分）。
 * @property type 交易类型（支出/收入/转账）。
 * @property category 关联的分类及图标。
 * @property time 交易时间（时分秒）。
 * @property currency 交易原始币种代码。
 * @property primaryAmount 折合为主币种后的金额（分）。
 * @property detail 用于执行账单操作的完整交易明细。
 */
data class AnalyticsTransaction(
    val id: Long,
    val date: LocalDate,
    val note: String,
    val amount: Long,
    val type: cn.turingzy.ledger.model.transaction.TransactionType = cn.turingzy.ledger.model.transaction.TransactionType.EXPENSE,
    val category: cn.turingzy.ledger.model.category.CategoryWithIcon? = null,
    val time: java.time.LocalTime? = null,
    val currency: String = "CNY",
    val primaryAmount: Long = amount,
    internal val detail: cn.turingzy.ledger.model.transaction.TransactionDetail? = null,
)

/**
 * 周期对比（如环比/同比）数据汇总模型。
 *
 * @property periodStart 对比周期的开始日期。
 * @property periodEnd 对比周期的结束日期。
 * @property totalExpense 对比周期的总支出（分）。
 * @property totalIncome 对比周期的总收入（分）。
 */
data class AnalyticsComparison(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalExpense: Long,
    val totalIncome: Long,
) {
    /** 对比周期的结余金额（总收入 - 总支出）。 */
    val balance: Long get() = totalIncome - totalExpense
}

/**
 * 统计分析界面的 UI 状态接口。
 */
sealed interface AnalyticsUiState {
    /**
     * 数据加载中状态。
     */
    data object Loading : AnalyticsUiState

    /**
     * 数据加载完成状态。
     *
     * @property timeScope 当前选中的时间粒度。
     * @property categoryType 当前分析的收支类型（支出或收入）。
     * @property periodDisplay 界面顶部展示的周期文本（如“2026年11月”）。
     * @property totalExpense 周期总支出（分）。
     * @property totalIncome 周期总收入（分）。
     * @property balance 周期结余（分）。
     * @property rankings 分类排行榜数据列表。
     * @property periodStart 当前统计周期的起始日期。
     * @property periodEnd 当前统计周期的结束日期。
     * @property comparison 上一周期对比数据（若有时）。
     * @property currency 当前主币种代码。
     * @property amountsAvailable 主币种金额是否均计算就绪（汇率是否完备）。
     */
    data class Success(
        val timeScope: AnalyticsTimeScope,
        val categoryType: CategoryType, // 支出分析 vs 收入分析
        val periodDisplay: String,
        val totalExpense: Long,
        val totalIncome: Long,
        val balance: Long,
        val rankings: List<CategoryExpenseRanking>,
        val periodStart: LocalDate,
        val periodEnd: LocalDate = when (timeScope) {
            AnalyticsTimeScope.WEEK -> periodStart.plusDays(6)
            AnalyticsTimeScope.MONTH -> periodStart.withDayOfMonth(periodStart.lengthOfMonth())
            AnalyticsTimeScope.THREE_MONTHS -> periodStart.plusMonths(3).minusDays(1)
            AnalyticsTimeScope.YEAR -> LocalDate.of(periodStart.year, 12, 31)
            AnalyticsTimeScope.ALL -> periodStart
        },
        val comparison: AnalyticsComparison? = null,
        val currency: String = "CNY",
        val amountsAvailable: Boolean = true,
    ) : AnalyticsUiState
}
