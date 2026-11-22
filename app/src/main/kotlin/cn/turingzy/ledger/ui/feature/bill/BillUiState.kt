package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.model.transaction.DateFilterType
import cn.turingzy.ledger.model.transaction.TransactionDetail
import java.time.LocalDate

/**
 * 某一自然日内在指定币种下的支出与收入汇总。
 *
 * @property currency 货币代码（如 CNY, USD）。
 * @property expense 当日该币种的原币支出总计（分）。
 * @property income 当日该币种的原币收入总计（分）。
 */
data class DayCurrencyAmounts(val currency: String, val expense: Long, val income: Long)

/**
 * 账单列表单日分组数据模型。
 *
 * @property dateEpochDay 该日期的纪元日天数（[LocalDate.toEpochDay]）。
 * @property displayDate 显示的日期文案（例如 "11/22 周日"）。
 * @property totalExpense 该日折算为主币种后的总支出（分）。
 * @property totalIncome 该日折算为主币种后的总收入（分）。
 * @property transactions 该日包含的所有交易详情列表。
 * @property currencyAmounts 该日按原始币种统计的收支列表。
 */
data class DayTransactionGroup(
    val dateEpochDay: Long,
    val displayDate: String,
    val totalExpense: Long,
    val totalIncome: Long,
    val transactions: List<TransactionDetail>,
    val currencyAmounts: List<DayCurrencyAmounts> = emptyList(),
)

/**
 * 账单主页面 UI 状态。
 */
sealed interface BillUiState {
    /**
     * 账单数据正在加载中的状态。
     */
    data object Loading : BillUiState

    /**
     * 账单数据加载成功状态。
     *
     * @property year 当前统计周期的基准年份。
     * @property month 当前统计周期的基准月份。
     * @property monthDisplay 顶部周期药丸按钮展示的文本（例如 "2026年 11月"）。
     * @property cardMonthDisplay 顶部汇总卡片展示的标题文本（例如 "11月 结余"）。
     * @property monthIncome 当前周期内折算为主币种的总收入（分）。
     * @property monthExpense 当前周期内折算为主币种的总支出（分）。
     * @property monthBalance 当前周期内折算为主币种的净结余（分）。
     * @property dayGroups 当前周期内按天聚合的分组交易数据列表。
     * @property filterType 当前生效的日期过滤类型（月、年等）。
     * @property filterStartDate 过滤周期的开始日期（包含）。
     * @property filterEndDate 过滤周期的结束日期（包含）。
     * @property currency 主货币代码。
     * @property amountsAvailable 所需的汇率与折算金额是否有效可用（若缺失汇率则为 false）。
     */
    data class Success(
        val year: Int,
        val month: Int,
        val monthDisplay: String, // e.g. "2026年 11月"
        val cardMonthDisplay: String, // e.g. "11月 结余"
        val monthIncome: Long,
        val monthExpense: Long,
        val monthBalance: Long,
        val dayGroups: List<DayTransactionGroup>,
        val filterType: DateFilterType = DateFilterType.MONTH,
        val filterStartDate: LocalDate? = null,
        val filterEndDate: LocalDate? = null,
        val currency: String = "CNY",
        val amountsAvailable: Boolean = true,
    ) : BillUiState
}
