package cn.turingzy.ledger.ui.feature.bill

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BillTransactionsTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun dailyHeaderTogglesRowsAndRowOpensSelectedTransaction() {
        var selectedId: Long? = null
        val detail = TransactionDetail(
            Transaction(id = 42L, type = TransactionType.EXPENSE, amount = 123L, accountId = 1L, categoryId = 1L, transactionTime = 0L),
            Account(id = 1L, name = "测试账户", type = AccountType.DEBIT),
            category = CategoryWithIcon(
                Category(id = 1L, name = "测试餐饮", type = CategoryType.EXPENSE, iconId = 1L),
                Icon(id = 1L, keyName = "food", displayName = "餐饮", iconUri = "res://"),
            ),
        )
        compose.setContent {
            MaterialTheme {
                DayGroupSection(DayTransactionGroup(0L, "今天", 123L, 0L, listOf(detail)), onTransactionClick = { selectedId = it })
            }
        }
        compose.onNodeWithText("测试餐饮").assertIsDisplayed()
        compose.onNodeWithContentDescription("隐藏当日交易").performClick()
        compose.onNodeWithText("01/01", substring = true).assertIsDisplayed()
        compose.onNodeWithText("测试餐饮").assertDoesNotExist()
        compose.onNodeWithContentDescription("显示当日交易").performClick()
        compose.onNodeWithText("测试餐饮").performClick()
        assertEquals(42L, selectedId)
    }
    @Test
    fun zeroIncomeIsOmittedFromDailySummary() {
        showSummary(expense = 123L, income = 0L)
        compose.onNodeWithText("支出: 1.23").assertIsDisplayed()
        compose.onNodeWithText("收入:", substring = true).assertDoesNotExist()
    }

    @Test
    fun zeroExpenseIsOmittedFromDailySummary() {
        showSummary(expense = 0L, income = 456L)
        compose.onNodeWithText("收入: 4.56").assertIsDisplayed()
        compose.onNodeWithText("支出:", substring = true).assertDoesNotExist()
    }

    @Test
    fun zeroTotalsKeepDateAndCollapseControlWithoutSummary() {
        showSummary(expense = 0L, income = 0L)
        compose.onNodeWithText("01/01", substring = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("隐藏当日交易").assertExists()
        compose.onNodeWithText("支出:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("收入:", substring = true).assertDoesNotExist()
    }

    @Test
    fun nonzeroIncomeAndExpenseAreBothShown() {
        showSummary(expense = 123L, income = 456L)
        compose.onNodeWithText("支出: 1.23  收入: 4.56").assertIsDisplayed()
    }

    private fun showSummary(expense: Long, income: Long) {
        compose.setContent {
            MaterialTheme { DayGroupSection(DayTransactionGroup(0L, "今天", expense, income, emptyList())) }
        }
    }

}
