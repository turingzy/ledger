package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import cn.turingzy.ledger.model.LedgerCardAppearance
import cn.turingzy.ledger.ui.component.SummaryCard
import cn.turingzy.ledger.ui.component.SummaryCardContent

@Composable
fun SummaryHeroBanner(
    cardMonthDisplay: String,
    balance: Long,
    expense: Long,
    income: Long,
    modifier: Modifier = Modifier,
    appearance: LedgerCardAppearance = LedgerCardAppearance(),
    background: ImageBitmap? = null,
    settingsEnabled: Boolean = true,
    onUploadBackground: () -> Unit = {},
    onTransparencyChange: (Int) -> Unit = {},
) {
    var amountsHidden by rememberSaveable { mutableStateOf(false) }
    SummaryCard(
        modifier = modifier,
        appearance = appearance,
        background = background,
        settingsEnabled = settingsEnabled,
        onUploadBackground = onUploadBackground,
        onTransparencyChange = onTransparencyChange,
    ) {
        SummaryCardContent(
            title = cardMonthDisplay.ifBlank { "结余" },
            amount = balance,
            amountsHidden = amountsHidden,
            onToggleAmounts = { amountsHidden = !amountsHidden },
            leftAmount = expense,
            rightAmount = income,
        )
    }
}
