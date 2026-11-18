package cn.turingzy.ledger.ui.feature.settings.screen

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.ui.theme.LedgerTheme

internal data class GeneralSettingsPreviewCase(val busy: Boolean, val rate: String, val message: String? = null)

internal class GeneralSettingsPreviewProvider : PreviewParameterProvider<GeneralSettingsPreviewCase> {
    override val values: Sequence<GeneralSettingsPreviewCase> = sequenceOf(
        GeneralSettingsPreviewCase(false, "7.20"),
        GeneralSettingsPreviewCase(false, "7.25"),
        GeneralSettingsPreviewCase(false, "无效输入"),
        GeneralSettingsPreviewCase(true, "7.20", "正在刷新汇率"),
    )
}

@Preview(name = "General Settings Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "General Settings Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun GeneralSettingsPreview(@PreviewParameter(GeneralSettingsPreviewProvider::class) case: GeneralSettingsPreviewCase) {
    LedgerTheme {
        GeneralSettingsContent(
            draft = AppPreferences(primaryCurrency = "CNY"), busy = case.busy,
            from = "USD", rate = case.rate, rateDate = "2026-11-17", message = case.message,
            currencies = listOf("USD" to "USD", "HKD" to "HKD", "EUR" to "EUR"),
            onPreferencesChange = {}, onFromChange = {}, onRateChange = {}, onRefresh = {}, onBack = {},
        )
    }
}
