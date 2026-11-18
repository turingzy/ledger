package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test

class BillBatchOptionsTest {
    @Test
    fun mixedSelectionOffersCompatibleSubsetsAndExcludesTransferDestination() {
        val yuan = Account(id = 1L, name = "Yuan", type = AccountType.CASH)
        val dollars = Account(id = 2L, name = "Dollars", type = AccountType.CASH, currency = "USD")
        val savings = Account(id = 3L, name = "Savings", type = AccountType.CASH)
        val icon = Icon(id = 1L, keyName = "test", displayName = "Test", iconUri = "res://")
        val food = CategoryWithIcon(Category(1L, "Food", CategoryType.EXPENSE, 1L), icon)
        val salary = CategoryWithIcon(Category(2L, "Salary", CategoryType.INCOME, 1L), icon)
        val transfer = CategoryWithIcon(Category(3L, "Transfer", CategoryType.TRANSFER, 1L), icon)
        val details = listOf(
            TransactionDetail(Transaction(1L, TransactionType.EXPENSE, 100L, yuan.id, categoryId = food.category.id, transactionTime = 0L), yuan, category = food),
            TransactionDetail(Transaction(2L, TransactionType.INCOME, 200L, dollars.id, categoryId = salary.category.id, transactionTime = 0L), dollars, category = salary),
            TransactionDetail(Transaction(3L, TransactionType.TRANSFER, 300L, yuan.id, savings.id, transfer.category.id, 0L), yuan, savings, transfer),
        )
        val options = billBatchOptions(details, listOf(yuan, dollars, savings).map { AccountWithTemplate(it) }, listOf(food, salary, transfer))

        assertEquals(mapOf(1L to setOf(1L, 3L), 2L to setOf(2L), 3L to setOf(1L)), options.accounts)
        assertEquals(mapOf(1L to setOf(1L), 2L to setOf(2L), 3L to setOf(3L)), options.categories)
    }
}
