package cn.turingzy.ledger.ui.feature.settings.screen

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme

internal enum class SettingsSubpagePreviewCase { CardDesigns, NewCardDesign, ImportExport, About, Licenses }

internal class SettingsSubpagePreviewProvider : PreviewParameterProvider<SettingsSubpagePreviewCase> {
    override val values: Sequence<SettingsSubpagePreviewCase> = SettingsSubpagePreviewCase.entries.asSequence()
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "SettingsSubpage Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "SettingsSubpage Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsSubpagePreview(@PreviewParameter(SettingsSubpagePreviewProvider::class) case: SettingsSubpagePreviewCase) {
    val account = Account(1L, "日常账户", AccountType.DEBIT, currentBalance = 1234500L, cardNumber = "1234")
    val accounts = listOf(AccountWithTemplate(account), AccountWithTemplate(account.copy(id = 2L, name = "外币账户", currency = "USD")))
    val templates = listOf(CardTemplate(1L, "示例卡面", ""))
    LedgerTheme {
        when (case) {
            SettingsSubpagePreviewCase.CardDesigns -> CardTemplateManagementScreen(templates, {}, { 1L }, {}, {}, { null })
            SettingsSubpagePreviewCase.NewCardDesign -> NewCardTemplatePage(
                CardTemplate(-1L, "", ""), {}, {}, {}, {}, saving = false,
            )
            SettingsSubpagePreviewCase.ImportExport -> BillTransferScreen(defaultParseRulePreviews(), {}, accounts)
            SettingsSubpagePreviewCase.About -> AboutScreen("1.0.0", {})
            SettingsSubpagePreviewCase.Licenses -> OpenSourceLibrariesScreen({})
        }
    }
}
