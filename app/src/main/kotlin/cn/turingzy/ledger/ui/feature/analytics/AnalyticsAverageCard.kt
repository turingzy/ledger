package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
internal fun AnalyticsAverageCard(uiState: AnalyticsUiState.Success, modifier: Modifier = Modifier) {
    val average = remember(uiState.totalExpense, uiState.totalIncome, uiState.periodStart, uiState.periodEnd) {
        analyticsDailyAverage(uiState.totalExpense, uiState.totalIncome, uiState.periodStart, uiState.periodEnd)
    }
    val colors = MaterialTheme.colorScheme
    AnalyticsChartCard(modifier, contentSpacing = 12.dp) {
        AnalyticsCardTitle("日均收支")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AverageAmount("平均支出", average.expense, Modifier.weight(1f))
            AverageAmount("平均收入", average.income, Modifier.weight(1f))
        }
        VicoColumnChart(
            dataSeries = listOf(listOf(average.expense, average.income)),
            labels = listOf("支出", "收入"),
            seriesColors = listOf(colors.error, colors.tertiary),
            colorByCategory = true,
            description = "日均收支柱状图",
            modifier = Modifier.height(148.dp),
        )
    }
}

@Composable
private fun AverageAmount(label: String, amount: Long, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 13.sp, color = colors.onSurfaceVariant)
        Text(
            amount.toFormattedAmount(true),
            fontSize = 17.sp,
            textAlign = TextAlign.End,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
