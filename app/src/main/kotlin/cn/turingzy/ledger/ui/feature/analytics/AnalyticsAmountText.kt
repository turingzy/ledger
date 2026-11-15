package cn.turingzy.ledger.ui.feature.analytics

import cn.turingzy.ledger.ui.component.LocalLedgerCurrency
import cn.turingzy.ledger.ui.component.currencySymbol
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
internal fun AnalyticsAmountText(amount: Long, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp) {
    val currencySymbol = currencySymbol(LocalLedgerCurrency.current)
    Text(
        amount.toFormattedAmount(showSymbol = true, symbol = currencySymbol), modifier,
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize, lineHeight = 18.sp, fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface, fontFeatureSettings = "tnum",
        ),
        textAlign = textAlign, maxLines = 1, overflow = TextOverflow.Ellipsis,
    )
}
