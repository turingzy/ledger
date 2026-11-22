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
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import java.time.LocalDate
import java.time.YearMonth

/** 统计卡片顶部外边距。 */
internal val AnalyticsCardTopSpacing = 3.dp
/** 统计图表数据节点圆点半径。 */
internal val AnalyticsChartNodeRadius = 3.dp
/** 统计图表绘图区标准高度。 */
internal val AnalyticsChartPlotHeight = 170.dp
/** 统计图表网格间隔数。 */
internal const val AnalyticsChartGridIntervals = 8

/**
 * 统计看板卡片列表容器。
 *
 * 聚合渲染统计概览卡片、分类占比卡片、支出进度趋势卡片以及月度收支现金流卡片。
 *
 * @param state 当前周期的统计分析状态。
 * @param dashboard 全局统计看板数据状态。
 * @param onCategoryTypeChange 分类收支类型变更回调。
 */
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

/**
 * 带标题的统计看板卡片容器。
 *
 * @param title 卡片标题。
 * @param content 卡片内部内容。
 */
@Composable
internal fun DashboardCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    DashboardCard {
        AnalyticsCardTitle(title)
        content()
    }
}

/**
 * 统计看板通用卡片容器。
 *
 * @param modifier 修饰符。
 * @param contentSpacing 内容纵向间距。
 * @param content 卡片内部内容。
 */
@Composable
internal fun DashboardCard(
    modifier: Modifier = Modifier,
    contentSpacing: androidx.compose.ui.unit.Dp = LedgerStyle.spacing.sectionGap,
    content: @Composable ColumnScope.() -> Unit,
) {
    CardBase(
        modifier.padding(top = AnalyticsCardTopSpacing),
        shape = LedgerStyle.shapes.summaryCard,
        outlined = true,
        contentPadding = PaddingValues(horizontal = LedgerStyle.spacing.cardPadding, vertical = LedgerStyle.spacing.medium),
        contentSpacing = contentSpacing,
        content = content,
    )
}

/**
 * 统计卡片统一标题组件。
 *
 * @param title 标题文案。
 * @param modifier 修饰符。
 */
@Composable
internal fun AnalyticsCardTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, modifier, style = MaterialTheme.typography.titleMedium.copy(
        fontSize = LedgerStyle.typography.body.fontSize, lineHeight = 20.sp, fontWeight = FontWeight.Normal,
    ), maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * 获取统计图表中信息提示浮层或高亮气泡的背景颜色。
 *
 * @return 对应的背景 [Color]。
 */
@Composable
internal fun analyticsChartInfoColor(): Color {
    return LedgerStyle.colors.chartInfoBackground
}
