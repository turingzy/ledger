package cn.turingzy.ledger.ui.component.picker

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "DatePicker Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "DatePicker Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DatePickerPreview() {
    val date = LocalDate.of(2026, 11, 17)
    LedgerTheme {
        LedgerDatePickerDialog(date, {}, {})
    }
}
