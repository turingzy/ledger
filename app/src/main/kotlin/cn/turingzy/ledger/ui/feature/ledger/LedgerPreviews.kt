package cn.turingzy.ledger.ui.feature.ledger

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import java.time.ZoneId

internal data class LedgerPreviewCase(val state: LedgerUiState, val searchActive: Boolean = false)

internal class LedgerPreviewProvider : PreviewParameterProvider<LedgerPreviewCase> {
    override val values: Sequence<LedgerPreviewCase> = sequence {
        val date = LocalDate.of(2026, 11, 14)
        val empty = LedgerUiState.Success(2026, 11, "2026.11", "11月结余", 0L, 0L, 0L, emptyList())
        val account = Account(1L, "银行卡", AccountType.DEBIT)
        val category = CategoryWithIcon(
            Category(1L, "餐饮", CategoryType.EXPENSE, 1L),
            Icon(1L, "ic_food", "餐饮", "res://"),
        )
        val transaction = Transaction(
            id = 1L, type = TransactionType.EXPENSE, amount = 12345L, accountId = 1L, categoryId = 1L,
            transactionTime = date.atTime(12, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), note = "午餐",
        )
        val detail = TransactionDetail(transaction, account, category = category)
        val day = DayTransactionGroup(date.toEpochDay(), "11/14 周六", 12345L, 0L, listOf(detail))
        val populated = empty.copy(monthExpense = 12345L, monthBalance = -12345L, dayGroups = listOf(day))
        yield(LedgerPreviewCase(LedgerUiState.Loading))
        yield(LedgerPreviewCase(empty))
        yield(LedgerPreviewCase(populated))
        yield(LedgerPreviewCase(populated, searchActive = true))
        yield(LedgerPreviewCase(populated.copy(amountsAvailable = false)))
        val foreignDay = day.copy(transactions = listOf(detail.copy(account = account.copy(currency = "USD"))))
        yield(LedgerPreviewCase(populated.copy(dayGroups = listOf(foreignDay))))
        val longNoteDay = day.copy(transactions = listOf(
            detail.copy(transaction = transaction.copy(note = "较长的备注用于核对分类与金额列的空间分配")),
        ))
        yield(LedgerPreviewCase(populated.copy(dayGroups = listOf(longNoteDay))))
    }
}

@Preview(name = "Ledger Light", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Ledger Dark", showBackground = true, showSystemUi = true, widthDp = 420, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LedgerPreview(@PreviewParameter(LedgerPreviewProvider::class) case: LedgerPreviewCase) {
    LedgerTheme { LedgerContent(case.state, initialSearchActive = case.searchActive) }
}
