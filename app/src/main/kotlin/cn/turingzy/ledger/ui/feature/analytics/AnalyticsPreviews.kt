package cn.turingzy.ledger.ui.feature.analytics

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate

internal data class AnalyticsPreviewCase(
    val state: AnalyticsUiState,
    val dashboard: AnalyticsDashboardState,
)

internal class AnalyticsPreviewProvider : PreviewParameterProvider<AnalyticsPreviewCase> {
    override val values: Sequence<AnalyticsPreviewCase> = sequence {
        val start = LocalDate.of(2026, 11, 1)
        val empty = AnalyticsUiState.Success(
            AnalyticsTimeScope.MONTH, CategoryType.EXPENSE, "2026年11月",
            0L, 0L, 0L, emptyList(), start,
        )
        val ready = AnalyticsDashboardState(loading = false)
        yield(AnalyticsPreviewCase(AnalyticsUiState.Loading, ready))
        yield(AnalyticsPreviewCase(empty, ready.copy(loading = true)))
        yield(AnalyticsPreviewCase(empty, ready))
        yield(AnalyticsPreviewCase(empty.copy(amountsAvailable = false), ready))
        val transactions = listOf(
            AnalyticsTransaction(1L, start.plusDays(2), "午餐", "银行卡", 12500L),
            AnalyticsTransaction(2L, start.plusDays(3), "工资", "银行卡", 600000L, TransactionType.INCOME),
            AnalyticsTransaction(3L, start.plusDays(4), "外币消费", "银行卡", 1000L, currency = "USD", primaryAmount = 7200L),
        )
        val dashboard = ready.copy(
            days = mapOf(
                start.plusDays(2) to AnalyticsDay(12500L, 0L),
                start.plusDays(3) to AnalyticsDay(0L, 600000L),
                start.plusDays(4) to AnalyticsDay(7200L, 0L),
            ),
            transactions = transactions,
        )
        for (scope in listOf(AnalyticsTimeScope.WEEK, AnalyticsTimeScope.MONTH, AnalyticsTimeScope.YEAR, AnalyticsTimeScope.ALL)) {
            val end = when (scope) {
                AnalyticsTimeScope.WEEK -> start.plusDays(6)
                AnalyticsTimeScope.YEAR -> start.withMonth(12).withDayOfMonth(31)
                else -> start.withDayOfMonth(30)
            }
            for (type in listOf(CategoryType.EXPENSE, CategoryType.INCOME)) {
                val income = type == CategoryType.INCOME
                val entries = transactions.filter { (it.type == TransactionType.INCOME) == income }
                yield(AnalyticsPreviewCase(empty.copy(
                    timeScope = scope, categoryType = type, periodEnd = end,
                    periodDisplay = when (scope) {
                        AnalyticsTimeScope.WEEK -> "11月1日 — 11月7日"
                        AnalyticsTimeScope.YEAR -> "2026年"
                        else -> "2026年11月"
                    },
                    totalExpense = 19700L, totalIncome = 600000L, balance = 580300L,
                    rankings = listOf(CategoryExpenseRanking(1L, if (income) "工资" else "餐饮", if (income) 600000L else 19700L, 100f, entries)),
                ), dashboard))
            }
        }
    }
}

@Preview(name = "Analytics Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Analytics Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun AnalyticsPreview(@PreviewParameter(AnalyticsPreviewProvider::class) case: AnalyticsPreviewCase) {
    LedgerTheme { AnalyticsContent(case.state, {}, {}, dashboard = case.dashboard) }
}
