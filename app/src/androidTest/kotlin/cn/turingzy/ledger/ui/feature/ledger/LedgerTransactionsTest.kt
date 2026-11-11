package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LedgerTransactionsTest {
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
        compose.onNodeWithText("今天").assertIsDisplayed()
        compose.onNodeWithText("测试餐饮").assertDoesNotExist()
        compose.onNodeWithContentDescription("显示当日交易").performClick()
        compose.onNodeWithText("测试餐饮").performClick()
        assertEquals(42L, selectedId)
    }
}
