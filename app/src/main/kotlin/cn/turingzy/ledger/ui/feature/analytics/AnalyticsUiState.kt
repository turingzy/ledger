package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.model.CategoryType

enum class AnalyticsTimeScope {
    WEEK,
    MONTH,
    YEAR,
}

data class CategoryExpenseRanking(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: Long,
    val percentage: Float, // 0.0 ~ 1.0
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
    ) : AnalyticsUiState
}
