package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.model.CategoryType
import java.time.LocalDate

enum class AnalyticsTimeScope {
    WEEK,
    MONTH,
    THREE_MONTHS,
    YEAR,
    ALL,
}

data class CategoryExpenseRanking(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: Long,
    val percentage: Float,
    val transactions: List<AnalyticsTransaction> = emptyList(),
)

data class AnalyticsTransaction(
    val id: Long,
    val date: LocalDate,
    val note: String,
    val accountName: String,
    val amount: Long,
    val type: cn.turingzy.ledger.model.TransactionType = cn.turingzy.ledger.model.TransactionType.EXPENSE,
    val category: cn.turingzy.ledger.model.CategoryWithIcon? = null,
    val time: java.time.LocalTime? = null,
)

data class AnalyticsComparison(
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalExpense: Long,
    val totalIncome: Long,
) {
    val balance: Long get() = totalIncome - totalExpense
}

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState

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
    ) : AnalyticsUiState
}
