package cn.turingzy.ledger.ui.feature.analytics.component

import cn.turingzy.ledger.ui.feature.analytics.AnalyticsDashboardState
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsUiState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import cn.turingzy.ledger.ui.component.base.BaseCard
import cn.turingzy.ledger.ui.theme.LedgerStyle
import java.time.LocalDate
import java.time.YearMonth

internal val AnalyticsCardTopSpacing = 3.dp
internal val AnalyticsChartNodeRadius = 3.dp
internal val AnalyticsChartPlotHeight = 170.dp
internal const val AnalyticsChartGridIntervals = 8

@Composable
internal fun AnalyticsDashboardCards(state: AnalyticsUiState.Success, dashboard: AnalyticsDashboardState, onCategoryTypeChange: (cn.turingzy.ledger.model.category.CategoryType) -> Unit = {}) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    if (state.currency != dashboard.currency) return
    if (!state.amountsAvailable || !dashboard.amountsAvailable) {
        Text(textResources.getString(cn.turingzy.ledger.R.string.ui_exchange_rate_needed_refresh_or_enter_a_rate), color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val today = LocalDate.now()
    val anchor = minOf(state.periodEnd, today)
    AnalyticsStatisticsCard(state, dashboard.days)
    AnalyticsCategoryShareCard(state, onCategoryTypeChange)
    DashboardCard(textResources.getString(cn.turingzy.ledger.R.string.ui_cumulative_expenses)) {
        ExpensePace(dashboard.days, state.timeScope, state.periodStart, state.periodEnd, today)
    }
    MonthlyCashflowCard(dashboard.days, YearMonth.from(anchor), transactions = dashboard.transactions)
}

@Composable
internal fun DashboardCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    DashboardCard {
        AnalyticsCardTitle(title)
        content()
    }
}

@Composable
internal fun DashboardCard(
    modifier: Modifier = Modifier,
    contentSpacing: androidx.compose.ui.unit.Dp = LedgerStyle.spacing.sectionGap,
    content: @Composable ColumnScope.() -> Unit,
) {
    BaseCard(
        modifier.padding(top = AnalyticsCardTopSpacing),
        shape = LedgerStyle.shapes.summaryCard,
        outlined = true,
        contentPadding = PaddingValues(horizontal = LedgerStyle.spacing.cardPadding, vertical = LedgerStyle.spacing.medium),
        contentSpacing = contentSpacing,
        content = content,
    )
}

@Composable
internal fun AnalyticsCardTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier, style = MaterialTheme.typography.titleMedium.copy(
        fontSize = LedgerStyle.typography.body.fontSize, lineHeight = 20.sp, fontWeight = FontWeight.Normal,
    ), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
internal fun analyticsChartInfoColor(): Color {
    return LedgerStyle.colors.chartInfoBackground
}
