package cn.turingzy.ledger.ui.feature.bill.sheet

import cn.turingzy.ledger.ui.component.picker.LedgerDateTimeDialog
import android.content.res.Configuration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import java.time.ZoneId

internal enum class RecordTransactionPreviewCase { NewRecord, RecordDetails, Transfer, AccountPicker, RecordDateTime }

internal class RecordTransactionPreviewProvider : PreviewParameterProvider<RecordTransactionPreviewCase> {
    override val values: Sequence<RecordTransactionPreviewCase> = RecordTransactionPreviewCase.entries.asSequence()
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "RecordTransaction Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "RecordTransaction Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RecordTransactionPreview(@PreviewParameter(RecordTransactionPreviewProvider::class) case: RecordTransactionPreviewCase) {
    val account = Account(1L, "日常账户", AccountType.DEBIT, currentBalance = 1234500L, cardNumber = "1234")
    val accounts = listOf(AccountWithTemplate(account), AccountWithTemplate(account.copy(id = 2L, name = "外币账户", currency = "USD")))
    val category = CategoryWithIcon(Category(1L, "餐饮", CategoryType.EXPENSE, 1L), Icon(1L, "ic_food", "餐饮", "res://"))
    val date = LocalDate.of(2026, 11, 17)
    val time = date.atTime(12, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val transaction = Transaction(id = 1L, type = TransactionType.EXPENSE, amount = 12345L,
        accountId = 1L, categoryId = 1L, transactionTime = time, note = "午餐")
    LedgerTheme {
        when (case) {
            RecordTransactionPreviewCase.NewRecord, RecordTransactionPreviewCase.RecordDetails, RecordTransactionPreviewCase.Transfer -> RecordTransactionSheet(
                rememberModalBottomSheetState(skipPartiallyExpanded = true), listOf(category), accounts, {}, { _, _, _, _, _, _, _, _, _ -> },
                initialTransaction = when (case) {
                    RecordTransactionPreviewCase.RecordDetails -> transaction
                    RecordTransactionPreviewCase.Transfer -> transaction.copy(type = TransactionType.TRANSFER, toAccountId = 2L, transferRate = "0.14", toAmount = 1728L)
                    else -> null
                }, initialTransactionTime = time,
            )
            RecordTransactionPreviewCase.AccountPicker -> RecordAccountSheet("选择账户", accounts, 1L, TransactionType.EXPENSE, LedgerStyle.dimensions.dialogMaxHeight, {}, {})
            RecordTransactionPreviewCase.RecordDateTime -> LedgerDateTimeDialog(time, {}, {})
        }
    }
}
