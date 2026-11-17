package cn.turingzy.ledger.ui.feature.analytics.component

import cn.turingzy.ledger.ui.component.biz.LocalLedgerCurrency
import cn.turingzy.ledger.ui.component.biz.BusinessAmountText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
internal fun AnalyticsAmountText(amount: Long, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp) {
    BusinessAmountText(
        amount, LocalLedgerCurrency.current, modifier,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize, lineHeight = 18.sp, fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface, fontFeatureSettings = "tnum", textAlign = textAlign,
        ),
    )
}
