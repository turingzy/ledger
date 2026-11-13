package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth

internal val AnalyticsCardTopSpacing = 3.dp

@Composable
internal fun AnalyticsDashboardCards(state: AnalyticsUiState.Success, dashboard: AnalyticsDashboardState, onCategoryTypeChange: (cn.turingzy.ledger.model.CategoryType) -> Unit = {}) {
    val today = LocalDate.now()
    val anchor = minOf(state.periodEnd, today)
    AnalyticsStatisticsCard(state, dashboard.days)
    AnalyticsCategoryShareCard(state, onCategoryTypeChange)
    DashboardCard("累计支出对比") {
        ExpensePace(dashboard.days, state.timeScope, state.periodStart, state.periodEnd, today)
    }
    MonthlyCashflowCard(dashboard.days, YearMonth.from(anchor))
}

@Composable
internal fun DashboardCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().padding(top = AnalyticsCardTopSpacing)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}
