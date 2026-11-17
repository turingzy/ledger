package cn.turingzy.ledger.ui.component.biz

import android.content.res.Configuration
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.component.base.BaseCard
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.theme.LedgerTheme

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
@Preview(name = "Light", showBackground = true, widthDp = 420, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", showBackground = true, widthDp = 420, uiMode = Configuration.UI_MODE_NIGHT_YES)
private annotation class BusinessPreviewThemes

data class AmountPreviewCase(
    val label: String,
    val amount: Long? = 123456L,
    val currency: String = "CNY",
    val primary: String = "CNY",
    val type: TransactionType? = null,
    val hidden: Boolean = false,
    val grouping: Boolean = false,
    val size: BusinessAmountSize = BusinessAmountSize.Regular,
)

class AmountPreviewProvider : PreviewParameterProvider<AmountPreviewCase> {
    override val values: Sequence<AmountPreviewCase> = sequence {
        for (type in TransactionType.entries) {
            for (currency in listOf("CNY", "USD", "EUR", "JPY", "ZZZ")) {
                yield(AmountPreviewCase("$type / $currency", currency = currency, type = type))
            }
        }
        for (currency in listOf("USD", "EUR", "GBP", "JPY", "ZZZ")) {
            yield(AmountPreviewCase("Primary $currency", currency = currency, primary = currency))
        }
        yield(AmountPreviewCase("Zero", amount = 0L))
        for (type in TransactionType.entries) yield(AmountPreviewCase("Negative $type", amount = -1234L, type = type))
        yield(AmountPreviewCase("Missing", amount = null))
        yield(AmountPreviewCase("Hidden missing", amount = null, hidden = true))
        yield(AmountPreviewCase("Hidden value", hidden = true))
        yield(AmountPreviewCase("Grouped", grouping = true))
        yield(AmountPreviewCase("Long.MIN_VALUE", amount = Long.MIN_VALUE))
        yield(AmountPreviewCase("Long.MAX_VALUE", amount = Long.MAX_VALUE))
        for (size in BusinessAmountSize.entries) yield(AmountPreviewCase("Size $size", size = size))
    }
}

@BusinessPreviewThemes
@Composable
private fun AmountPreview(@PreviewParameter(AmountPreviewProvider::class) case: AmountPreviewCase) {
    LedgerTheme {
        BaseCard {
            Text(case.label, style = LedgerStyle.typography.caption)
            BusinessAmountText(
                case.amount, case.currency, type = case.type, primaryCurrency = case.primary,
                hidden = case.hidden, useGrouping = case.grouping, size = case.size,
            )
        }
    }
}

data class AccountPreviewCase(val label: String, val account: Account)

class AccountPreviewProvider : PreviewParameterProvider<AccountPreviewCase> {
    override val values: Sequence<AccountPreviewCase> = sequence {
        for (type in AccountType.entries) yield(AccountPreviewCase(type.name, Account(name = "示例账户", type = type)))
        for (name in listOf("支付宝", "微信", "其他钱包")) yield(AccountPreviewCase(name, Account(name = name, type = AccountType.WALLET)))
    }
}

@BusinessPreviewThemes
@Composable
private fun AccountPreview(@PreviewParameter(AccountPreviewProvider::class) case: AccountPreviewCase) {
    LedgerTheme {
        BaseCard {
            Text(case.label, style = LedgerStyle.typography.caption)
            BusinessAccountIcon(case.account)
        }
    }
}

private fun category(key: String, type: CategoryType = CategoryType.EXPENSE, deleted: Boolean = false): CategoryWithIcon =
    CategoryWithIcon(
        Category(id = 1L, name = key, type = type, iconId = 1L, isDeleted = deleted),
        Icon(id = 1L, keyName = key, displayName = key, iconUri = ""),
    )

data class CategoryPreviewCase(val category: CategoryWithIcon?, val type: TransactionType)

class CategoryPreviewProvider : PreviewParameterProvider<CategoryPreviewCase> {
    override val values: Sequence<CategoryPreviewCase> = sequence {
        for (key in listOf("ic_food", "ic_shopping", "ic_transport", "ic_entertainment", "ic_housing", "ic_medical",
            "ic_salary", "ic_investment", "ic_transfer", "ic_other", "unknown")) {
            val type = when (key) {
                "ic_salary", "ic_investment" -> CategoryType.INCOME
                "ic_transfer" -> CategoryType.TRANSFER
                else -> CategoryType.EXPENSE
            }
            val transactionType = TransactionType.valueOf(type.name)
            yield(CategoryPreviewCase(category(key, type), transactionType))
        }
        for (type in TransactionType.entries) yield(CategoryPreviewCase(null, type))
    }
}

@BusinessPreviewThemes
@Composable
private fun CategoryPreview(@PreviewParameter(CategoryPreviewProvider::class) case: CategoryPreviewCase) {
    LedgerTheme {
        BaseCard {
            BusinessTypeIcon(case.category?.icon?.categoryGlyph() ?: MaterialSymbols.MoreHoriz, case.type)
        }
    }
}

data class TransactionPreviewCase(val label: String, val detail: TransactionDetail, val hidden: Boolean = false)

class TransactionPreviewProvider : PreviewParameterProvider<TransactionPreviewCase> {
    override val values: Sequence<TransactionPreviewCase> = sequence {
        fun detail(type: TransactionType, currency: String = "CNY", note: String? = null): TransactionDetail =
            TransactionDetail(
                Transaction(id = 1L, type = type, amount = 12345L, accountId = 1L, toAccountId = 2L,
                    categoryId = 1L, transactionTime = 0L, note = note),
                Account(id = 1L, name = "来源账户", type = AccountType.DEBIT, currency = currency),
                Account(id = 2L, name = "目标账户", type = AccountType.WALLET, currency = currency),
                category(when (type) {
                    TransactionType.EXPENSE -> "ic_food"
                    TransactionType.INCOME -> "ic_salary"
                    TransactionType.TRANSFER -> "ic_transfer"
                }, CategoryType.valueOf(type.name)),
            )
        for (type in TransactionType.entries) for (currency in listOf("CNY", "USD")) for (note in listOf(null, "备注内容")) {
            yield(TransactionPreviewCase("$type / $currency / note=$note", detail(type, currency, note)))
        }
        val transfer = detail(TransactionType.TRANSFER)
        yield(TransactionPreviewCase("Same currency different received", transfer.copy(transaction = transfer.transaction.copy(toAmount = 12000L))))
        val cross = transfer.copy(toAccount = transfer.toAccount?.copy(currency = "USD"))
        yield(TransactionPreviewCase("Cross currency received", cross.copy(transaction = cross.transaction.copy(toAmount = 1700L))))
        yield(TransactionPreviewCase("Cross currency missing received", cross))
        yield(TransactionPreviewCase("Missing destination", transfer.copy(toAccount = null)))
        yield(TransactionPreviewCase("Hidden transfer", cross, hidden = true))
        yield(TransactionPreviewCase("Deleted account", transfer.copy(account = transfer.account.copy(isDeleted = true))))
        yield(TransactionPreviewCase("Deleted destination", transfer.copy(toAccount = transfer.toAccount?.copy(isDeleted = true))))
        yield(TransactionPreviewCase("Deleted category", transfer.copy(category = category("ic_transfer", CategoryType.TRANSFER, deleted = true))))
        yield(TransactionPreviewCase("Deleted transaction: no row", transfer.copy(transaction = transfer.transaction.copy(isDeleted = true))))
        yield(TransactionPreviewCase("Long text", detail(TransactionType.EXPENSE, note = "较长备注：用于核对长文本与金额列布局")))
    }
}

@BusinessPreviewThemes
@Composable
private fun TransactionPreview(@PreviewParameter(TransactionPreviewProvider::class) case: TransactionPreviewCase) {
    LedgerTheme {
        BaseCard {
            Text(case.label, style = LedgerStyle.typography.caption)
            val detail = case.detail
            val transaction = detail.transaction
            BusinessTransactionItem(
                amount = transaction.amount, currency = detail.account.currency, type = transaction.type,
                category = detail.category, accountName = detail.account.name, timeText = "00:00",
                note = transaction.note, primaryCurrency = "CNY", hidden = case.hidden,
                isDeleted = transaction.isDeleted, accountDeleted = detail.account.isDeleted,
                destinationName = detail.toAccount?.name, destinationCurrency = detail.toAccount?.currency,
                destinationDeleted = detail.toAccount?.isDeleted == true, receivedAmount = transaction.toAmount,
            )
        }
    }
}

@BusinessPreviewThemes
@Composable
private fun TransactionProjectionPreview() {
    LedgerTheme {
        BaseCard {
            BusinessTransactionItem(
                amount = 1234L, currency = "USD", type = TransactionType.EXPENSE, category = null,
                accountName = "投影账户", timeText = "09:30", note = "分类资料缺失，备注保留", primaryCurrency = "CNY",
            )
            BusinessTransactionItem(
                amount = 0L, currency = "CNY", type = TransactionType.INCOME, category = null,
                accountName = "投影账户", timeText = "", primaryCurrency = "CNY",
            )
            BusinessTransactionItem(
                amount = Long.MIN_VALUE, currency = "USD", type = TransactionType.EXPENSE,
                category = category("ic_food"), accountName = "长金额账户", timeText = "09:30", primaryCurrency = "CNY",
            )
        }
    }
}
