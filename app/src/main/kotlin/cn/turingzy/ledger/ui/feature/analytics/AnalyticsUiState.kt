package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.model.CategoryType
import java.time.LocalDate

enum class AnalyticsTimeScope {
    WEEK,
    MONTH,
    YEAR,
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
)

data class AnalyticsTrendPoint(
    val date: LocalDate,
    val expense: Long,
    val income: Long,
)

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState

    data class Success(
        val timeScope: AnalyticsTimeScope,
        val categoryType: CategoryType, // 支出分析 vs 收入分析
        val periodDisplay: String, // e.g. "2026年11月"
        val totalExpense: Long,
        val totalIncome: Long,
        val balance: Long,
        val rankings: List<CategoryExpenseRanking>,
        val periodStart: LocalDate,
        val trendPoints: List<AnalyticsTrendPoint> = emptyList(),
    ) : AnalyticsUiState
}
