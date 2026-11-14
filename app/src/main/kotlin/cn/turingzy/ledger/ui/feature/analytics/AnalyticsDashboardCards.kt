package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import java.time.LocalDate
import java.time.YearMonth

internal val AnalyticsCardTopSpacing = 3.dp
internal val AnalyticsChartNodeRadius = 3.dp
internal val AnalyticsChartPlotHeight = 170.dp
internal const val AnalyticsChartGridIntervals = 8

@Composable
internal fun AnalyticsDashboardCards(state: AnalyticsUiState.Success, dashboard: AnalyticsDashboardState, onCategoryTypeChange: (cn.turingzy.ledger.model.CategoryType) -> Unit = {}) {
    val today = LocalDate.now()
    val anchor = minOf(state.periodEnd, today)
    AnalyticsStatisticsCard(state, dashboard.days)
    AnalyticsCategoryShareCard(state, onCategoryTypeChange)
    DashboardCard("累计支出对比") {
        ExpensePace(dashboard.days, state.timeScope, state.periodStart, state.periodEnd, today)
    }
    MonthlyCashflowCard(dashboard.days, YearMonth.from(anchor), transactions = dashboard.transactions)
}

@Composable
internal fun DashboardCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().padding(top = AnalyticsCardTopSpacing)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnalyticsCardTitle(title)
            content()
        }
    }
}

@Composable
internal fun AnalyticsCardTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier, style = MaterialTheme.typography.titleMedium.copy(
        fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal,
    ), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun analyticsChartInfoColor(): Color {
    val colors = MaterialTheme.colorScheme
    return if (colors.surface.luminance() > 0.5f) lerp(colors.surface, colors.surfaceVariant, 0.48f)
    else colors.surfaceVariant
}
