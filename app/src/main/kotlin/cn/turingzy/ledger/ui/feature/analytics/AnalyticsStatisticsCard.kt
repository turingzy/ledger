package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Surface
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
    val average = remember(state, days) {
        val previous = previousAnalyticsRange(state.periodStart, state.periodEnd)
        val previousExpense = state.comparison?.totalExpense ?: days.filterKeys { it >= previous.start && it <= previous.end }.values.sumOf { it.expense }
        analyticsExpenseAverage(state.timeScope, state.totalExpense, state.periodStart, state.periodEnd,
            previousExpense, state.comparison?.periodStart ?: previous.start, state.comparison?.periodEnd ?: previous.end)
    }
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier.fillMaxWidth().padding(top = AnalyticsCardTopSpacing),
        shape = RoundedCornerShape(20.dp), color = colors.surface,
        border = BorderStroke(0.5.dp, colors.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("收支统计", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatisticsMetric("支出", state.totalExpense, Modifier.weight(1f))
                StatisticsMetric("收入", state.totalIncome, Modifier.weight(1f))
            }
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatisticsMetric("结余", state.balance, Modifier.weight(1f))
                StatisticsMetric(average.label, average.amount, Modifier.weight(1f), average)
            }
        }
    }
}

@Composable
private fun StatisticsMetric(label: String, amount: Long, modifier: Modifier = Modifier, average: AnalyticsExpenseAverage? = null) {
    val colors = MaterialTheme.colorScheme
    Surface(modifier.fillMaxHeight(), shape = RoundedCornerShape(14.dp), color = colors.surfaceVariant.copy(alpha = 0.6f)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, fontSize = 11.sp, lineHeight = 14.sp, color = colors.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AnalyticsAmountText(amount, Modifier.weight(1f))
                average?.takeIf { it.direction != 0 }?.let {
                    val color = if (it.direction > 0) colors.error else colors.tertiary
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        Icon(
                            if (it.direction > 0) MaterialSymbols.TrendingUp else MaterialSymbols.TrendingDown,
                            if (it.direction > 0) "环比增长" else "环比下降",
                            Modifier.size(10.dp), tint = color,
                        )
                        Text(it.change.substringAfter(' '), fontSize = 9.sp, lineHeight = 11.sp, color = color, maxLines = 1)
                    }
                }
            }
        }
    }
}
