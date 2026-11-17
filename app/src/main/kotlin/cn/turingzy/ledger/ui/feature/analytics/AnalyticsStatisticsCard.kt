package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.components.base.BaseSurface
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.ui.component.MaterialSymbols
import java.time.LocalDate

@Composable
internal fun AnalyticsStatisticsCard(state: AnalyticsUiState.Success, days: Map<LocalDate, AnalyticsDay>, modifier: Modifier = Modifier) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val average = remember(state, days) {
        val previous = previousAnalyticsRange(state.periodStart, state.periodEnd)
        val previousExpense = state.comparison?.totalExpense ?: days.filterKeys { it >= previous.start && it <= previous.end }.values.sumOf { it.expense }
        analyticsExpenseAverage(state.timeScope, state.totalExpense, state.periodStart, state.periodEnd,
            previousExpense, state.comparison?.periodStart ?: previous.start, state.comparison?.periodEnd ?: previous.end)
    }
    DashboardCard(modifier) {
        AnalyticsCardTitle(textResources.getString(cn.turingzy.ledger.R.string.ui_income_and_expenses))
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatisticsMetric(textResources.getString(cn.turingzy.ledger.R.string.ui_expense), state.totalExpense, Modifier.weight(1f))
            StatisticsMetric(textResources.getString(cn.turingzy.ledger.R.string.ui_income), state.totalIncome, Modifier.weight(1f))
        }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatisticsMetric(textResources.getString(cn.turingzy.ledger.R.string.ui_balance), state.balance, Modifier.weight(1f))
            StatisticsMetric(when (average.label) {
                "月均支出" -> textResources.getString(cn.turingzy.ledger.R.string.ui_monthly_average)
                "年均支出" -> textResources.getString(cn.turingzy.ledger.R.string.ui_yearly_average)
                else -> textResources.getString(cn.turingzy.ledger.R.string.ui_daily_average)
            }, average.amount, Modifier.weight(1f), average)
        }
    }
}

@Composable
private fun StatisticsMetric(label: String, amount: Long, modifier: Modifier = Modifier, average: AnalyticsExpenseAverage? = null) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    BaseSurface(modifier.fillMaxHeight(), shape = LedgerStyle.shapes.bankCard, color = analyticsChartInfoColor()) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, fontSize = LedgerStyle.typography.caption.fontSize, lineHeight = 14.sp, color = colors.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall)) {
                AnalyticsAmountText(amount, Modifier.weight(1f), fontSize = LedgerStyle.typography.bodyMedium.fontSize)
                average?.takeIf { it.direction != 0 }?.let {
                    val color = if (it.direction > 0) colors.error else colors.tertiary
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.tiny)) {
                        Icon(
                            if (it.direction > 0) MaterialSymbols.TrendingUp else MaterialSymbols.TrendingDown,
                            if (it.direction > 0) textResources.getString(cn.turingzy.ledger.R.string.ui_increase) else textResources.getString(cn.turingzy.ledger.R.string.ui_decrease),
                            Modifier.size(10.dp), tint = color,
                        )
                        Text(it.change.substringAfter(' '), fontSize = 9.sp, lineHeight = 11.sp, color = color, maxLines = 1)
                    }
                }
            }
        }
    }
}
