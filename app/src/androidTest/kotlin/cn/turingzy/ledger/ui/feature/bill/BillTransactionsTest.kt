package cn.turingzy.ledger.ui.feature.bill

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.feature.bill.component.BillSwipeRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BillTransactionsTest {
    @get:Rule val compose = createComposeRule()

    private val detail = TransactionDetail(
        Transaction(id = 42L, type = TransactionType.EXPENSE, amount = 123L, accountId = 1L, categoryId = 1L, transactionTime = 0L),
        Account(id = 1L, name = "测试账户", type = AccountType.DEBIT),
        category = CategoryWithIcon(
            Category(id = 1L, name = "测试餐饮", type = CategoryType.EXPENSE, iconId = 1L),
            Icon(id = 1L, keyName = "food", displayName = "餐饮", iconUri = "res://"),
        ),
    )

    @Test
    fun dailyHeaderStaysExpandedAndRowOpensSelectedTransaction() {
        var selectedId: Long? = null
        compose.setContent {
            MaterialTheme {
                DayGroupSection(DayTransactionGroup(0L, "今天", 123L, 0L, listOf(detail)), onTransactionClick = { selectedId = it })
            }
        }
        compose.onNodeWithText("测试餐饮").assertIsDisplayed()
        compose.onNodeWithText("01/01", substring = true).assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithContentDescription("隐藏当日交易").assertDoesNotExist()
        compose.onNodeWithContentDescription("显示当日交易").assertDoesNotExist()
        compose.onNodeWithText("测试餐饮").performClick()
        assertEquals(42L, selectedId)
    }
    @Test
    fun zeroIncomeIsOmittedFromDailySummary() {
        showSummary(expense = 123L, income = 0L)
        compose.onNodeWithText("支出: ¥1.23").assertIsDisplayed()
        compose.onNodeWithText("收入:", substring = true).assertDoesNotExist()
    }

    @Test
    fun zeroExpenseIsOmittedFromDailySummary() {
        showSummary(expense = 0L, income = 456L)
        compose.onNodeWithText("收入: ¥4.56").assertIsDisplayed()
        compose.onNodeWithText("支出:", substring = true).assertDoesNotExist()
    }

    @Test
    fun zeroTotalsKeepDateWithoutSummary() {
        showSummary(expense = 0L, income = 0L)
        compose.onNodeWithText("01/01", substring = true).assertIsDisplayed()
        compose.onNodeWithText("支出:", substring = true).assertDoesNotExist()
        compose.onNodeWithText("收入:", substring = true).assertDoesNotExist()
    }

    @Test
    fun nonzeroIncomeAndExpenseBothUseTheSelectedPrimaryCurrency() {
        showSummary(expense = 123L, income = 456L, currency = "USD")
        compose.onNodeWithText("支出: \$1.23  收入: \$4.56").assertIsDisplayed()
    }

    @Test
    fun swipingAnotherRowClosesTheFirstRowActions() {
        var copiedId: Long? = null
        compose.setContent {
            MaterialTheme {
                var revealedId by remember { mutableStateOf<Long?>(null) }
                Column {
                    for (id in listOf(42L, 43L)) Box(Modifier.testTag("transaction-$id")) {
                        BillSwipeRow(
                            detail = detail.copy(transaction = detail.transaction.copy(id = id)),
                            selecting = false,
                            selected = false,
                            enabled = true,
                            revealed = revealedId == id,
                            onRevealChange = { open -> if (open || revealedId == id) revealedId = id.takeIf { open } },
                            onClick = {},
                            onLongClick = {},
                            onCopy = { copiedId = id },
                            onEdit = {},
                            onDelete = {},
                        )
                    }
                }
            }
        }

        compose.onNodeWithTag("transaction-42").performTouchInput { swipeLeft() }
        compose.onNodeWithContentDescription("复制交易").assertIsDisplayed()
        compose.onNodeWithTag("transaction-43").performTouchInput { swipeLeft() }
        compose.onAllNodesWithContentDescription("复制交易").assertCountEquals(1)
        compose.onNodeWithContentDescription("复制交易").performClick()
        assertEquals(43L, copiedId)
    }

    @Test
    fun swipedForegroundExtendsPastTheDayGroupEdge() {
        compose.setContent {
            MaterialTheme {
                var revealed by remember { mutableStateOf(false) }
                Box(Modifier.padding(horizontal = 20.dp)) {
                    DayGroupSection(
                        DayTransactionGroup(0L, "今天", 123L, 0L, listOf(detail)),
                        modifier = Modifier.testTag("swipeDayGroup"),
                        transactionRow = { row, roundTop, roundBottom ->
                            BillSwipeRow(
                                detail = row,
                                selecting = false,
                                selected = false,
                                enabled = true,
                                revealed = revealed,
                                onRevealChange = { revealed = it },
                                onClick = {},
                                onLongClick = {},
                                onCopy = {},
                                onEdit = {},
                                onDelete = {},
                                roundTop = roundTop,
                                roundBottom = roundBottom,
                            )
                        },
                    )
                }
            }
        }

        val group = compose.onNodeWithTag("swipeDayGroup")
        val row = compose.onNodeWithText("测试餐饮")
        val groupLeft = group.fetchSemanticsNode().boundsInRoot.left
        row.performTouchInput { swipeLeft() }
        assertTrue(row.fetchSemanticsNode().boundsInRoot.left < groupLeft)
        compose.onNodeWithContentDescription("复制交易").assertIsDisplayed()
    }

    private fun showSummary(expense: Long, income: Long, currency: String = "CNY") {
        compose.setContent {
            MaterialTheme { DayGroupSection(DayTransactionGroup(0L, "今天", expense, income, emptyList()), currency = currency) }
        }
    }

}
