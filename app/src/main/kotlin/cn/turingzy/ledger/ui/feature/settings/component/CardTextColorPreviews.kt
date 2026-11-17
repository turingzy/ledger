package cn.turingzy.ledger.ui.feature.settings.component

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.turingzy.ledger.ui.theme.LedgerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "CardTextColor Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "CardTextColor Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CardTextColorPreview() {
    LedgerTheme {
        CardTextColorPicker("#FFFFFFFF", {})
    }
}
