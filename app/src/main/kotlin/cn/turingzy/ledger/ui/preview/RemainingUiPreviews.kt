package cn.turingzy.ledger.ui.preview

import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.model.*
import cn.turingzy.ledger.ui.component.CardBackgroundDialog
import cn.turingzy.ledger.ui.component.LedgerDatePickerDialog
import cn.turingzy.ledger.ui.feature.ledger.RecordAccountSheet
import cn.turingzy.ledger.ui.feature.ledger.RecordDateTimeDialog
import cn.turingzy.ledger.ui.feature.ledger.RecordTransactionSheet
import cn.turingzy.ledger.ui.feature.settings.*
import cn.turingzy.ledger.ui.feature.wallet.AccountCardFaceDialog
import cn.turingzy.ledger.ui.feature.wallet.AccountDetailsSheet
import cn.turingzy.ledger.ui.feature.wallet.AddAccountSheet
import cn.turingzy.ledger.ui.theme.LedgerTheme
import cn.turingzy.ledger.ui.theme.LedgerStyle
import java.time.LocalDate
import java.time.ZoneId

internal enum class RemainingUiCase {
    CardDesigns, NewCardDesign, ImportExport, BackupRestore, About, Licenses,
    NewRecord, RecordDetails, Transfer, AccountPicker, RecordDateTime,
    NewAccount, AccountDetails, CardFacePicker, CardBackground, DatePicker, TextColor,
}

internal class RemainingUiPreviewProvider : PreviewParameterProvider<RemainingUiCase> {
    override val values: Sequence<RemainingUiCase> = RemainingUiCase.entries.asSequence()
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Remaining UI Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Remaining UI Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RemainingUiPreview(@PreviewParameter(RemainingUiPreviewProvider::class) case: RemainingUiCase) {
    val account = Account(1L, "日常账户", AccountType.DEBIT, currentBalance = 1234500L, cardNumber = "1234")
    val accounts = listOf(AccountWithTemplate(account), AccountWithTemplate(account.copy(id = 2L, name = "外币账户", currency = "USD")))
    val category = CategoryWithIcon(Category(1L, "餐饮", CategoryType.EXPENSE, 1L), Icon(1L, "ic_food", "餐饮", "res://"))
    val templates = listOf(CardTemplate(1L, "示例卡面", ""))
    val date = LocalDate.of(2026, 11, 17)
    val time = date.atTime(12, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val transaction = Transaction(id = 1L, type = TransactionType.EXPENSE, amount = 12345L,
        accountId = 1L, categoryId = 1L, transactionTime = time, note = "午餐")
    LedgerTheme {
        when (case) {
            RemainingUiCase.CardDesigns, RemainingUiCase.NewCardDesign -> CardTemplateManagementScreen(
                if (case == RemainingUiCase.NewCardDesign) emptyList() else templates, {}, { 1L }, {}, {}, { null },
            )
            RemainingUiCase.ImportExport -> BillTransferScreen(defaultParseRulePreviews(), {}, accounts)
            RemainingUiCase.BackupRestore -> BackupAndRestoreScreen({}, { _, _, _ -> }, {})
            RemainingUiCase.About -> AboutScreen("1.0.0", {})
            RemainingUiCase.Licenses -> OpenSourceLibrariesScreen({})
            RemainingUiCase.NewRecord, RemainingUiCase.RecordDetails, RemainingUiCase.Transfer -> RecordTransactionSheet(
                rememberModalBottomSheetState(skipPartiallyExpanded = true), listOf(category), accounts, {}, { _, _, _, _, _, _, _, _ -> },
                initialTransaction = when (case) {
                    RemainingUiCase.RecordDetails -> transaction
                    RemainingUiCase.Transfer -> transaction.copy(type = TransactionType.TRANSFER, toAccountId = 2L, transferRate = "0.14", toAmount = 1728L)
                    else -> null
                }, initialTransactionTime = time,
            )
            RemainingUiCase.AccountPicker -> RecordAccountSheet("选择账户", accounts, 1L, TransactionType.EXPENSE, LedgerStyle.dimensions.dialogMaxHeight, {}, {})
            RemainingUiCase.RecordDateTime -> RecordDateTimeDialog(time, {}, {})
            RemainingUiCase.NewAccount -> AddAccountSheet({}, { _, _, _, _, _, _, _ -> }, templates = templates)
            RemainingUiCase.AccountDetails -> AccountDetailsSheet(account, {}, {}, {}, templates = templates)
            RemainingUiCase.CardFacePicker -> AccountCardFaceDialog(templates, null, account.name, account.type, "1234", {}, {})
            RemainingUiCase.CardBackground -> CardBackgroundDialog("bg_ledger_card.jpg", null, false, null, {}, {}, {})
            RemainingUiCase.DatePicker -> LedgerDatePickerDialog(date, {}, {})
            RemainingUiCase.TextColor -> CardTextColorPicker("#FFFFFFFF", {})
        }
    }
}
