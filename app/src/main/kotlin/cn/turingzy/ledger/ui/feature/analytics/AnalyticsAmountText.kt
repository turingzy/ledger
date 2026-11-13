package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
internal fun AnalyticsAmountText(amount: Long, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    Text(
        amount.toFormattedAmount(showSymbol = true), modifier,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = 16.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface, fontFeatureSettings = "tnum",
        ),
        textAlign = textAlign, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}
