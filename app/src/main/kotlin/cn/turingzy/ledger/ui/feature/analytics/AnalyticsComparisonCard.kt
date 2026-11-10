package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
internal fun AnalyticsComparisonCard(
    uiState: AnalyticsUiState.Success,
    comparison: AnalyticsComparison,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val (currentLabel, previousLabel) = when (uiState.timeScope) {
        AnalyticsTimeScope.WEEK -> "本周" to "上周"
        AnalyticsTimeScope.MONTH -> "本月" to "上月"
        AnalyticsTimeScope.THREE_MONTHS -> "这3个月" to "前3个月"
        AnalyticsTimeScope.YEAR -> "今年" to "去年"
    }
    AnalyticsChartCard(modifier, contentSpacing = 12.dp) {
        Text("收支对比", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Spacer(Modifier.width(42.dp))
                Text(currentLabel, Modifier.weight(1f), fontSize = 13.sp, textAlign = TextAlign.End, color = colors.onSurfaceVariant)
                Text(previousLabel, Modifier.weight(1f), fontSize = 13.sp, textAlign = TextAlign.End, color = colors.onSurfaceVariant)
                Text("增长", Modifier.width(58.dp), fontSize = 13.sp, textAlign = TextAlign.End, color = colors.onSurface)
            }
            ComparisonAmountRow("收入", uiState.totalIncome, comparison.totalIncome, increaseIsPositive = true)
            ComparisonAmountRow("支出", uiState.totalExpense, comparison.totalExpense, increaseIsPositive = false)
            HorizontalDivider(color = colors.outlineVariant)
            ComparisonAmountRow("净结余", uiState.balance, comparison.balance, increaseIsPositive = true)
        }
    }
}

@Composable
private fun ComparisonAmountRow(
    label: String,
    current: Long,
    previous: Long,
    increaseIsPositive: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val changeColor: Color = when {
        current == previous -> colors.onSurfaceVariant
        (current > previous) == increaseIsPositive -> colors.tertiary
        else -> colors.error
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(42.dp), fontSize = 13.sp, maxLines = 1)
        Text(
            current.toFormattedAmount(true), Modifier.weight(1f),
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            previous.toFormattedAmount(true), Modifier.weight(1f),
            fontSize = 12.sp, color = colors.onSurfaceVariant,
            textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        Text(
            comparisonChangeLabel(current, previous), Modifier.width(58.dp),
            fontSize = 13.sp, color = changeColor,
            textAlign = TextAlign.End, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}
