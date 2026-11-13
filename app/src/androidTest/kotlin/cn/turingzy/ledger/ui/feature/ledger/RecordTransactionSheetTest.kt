package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class RecordTransactionSheetTest {
    @get:Rule val compose = createComposeRule()
    private val accounts = listOf(
        AccountWithTemplate(Account(id = 1L, name = "测试账户", type = AccountType.DEBIT)),
        AccountWithTemplate(Account(id = 2L, name = "储蓄账户", type = AccountType.DEBIT)),
    )
    private val expenseCategories = List(40) { index ->
        CategoryWithIcon(
            Category(id = index + 1L, name = "分类$index", type = CategoryType.EXPENSE, iconId = 1L, sortOrder = index),
            Icon(id = 1L, keyName = "food", displayName = "餐饮", iconUri = "res://"),
        )
    }

    private val categories = expenseCategories + CategoryWithIcon(
        Category(id = 200L, name = "工资", type = CategoryType.INCOME, iconId = 1L),
        Icon(id = 1L, keyName = "salary", displayName = "工资", iconUri = "res://"),
    ) + CategoryWithIcon(
        Category(id = 100L, name = "转账分类", type = CategoryType.TRANSFER, iconId = 1L),
        Icon(id = 1L, keyName = "transfer", displayName = "转账", iconUri = "res://"),
    )

    @Test
    fun expenseAndIncomeSelectFirstCategoryOnOpeningAndTypeChange() {
        showSheet {}
        compose.onNodeWithText("分类0").assertIsSelected()
        compose.onNodeWithText("分类1").performClick()
        compose.onNodeWithText("收入").performClick()
        compose.onNodeWithText("工资").assertIsSelected()
        compose.onNodeWithText("支出").performClick()
        compose.onNodeWithText("分类0").assertIsSelected()
    }

    @Test
    fun editingKeepsExistingCategoryInsteadOfSelectingFirst() {
        showSheet(initialTransaction = Transaction(
            id = 9L, type = TransactionType.EXPENSE, amount = 100L,
            accountId = 1L, categoryId = 3L, transactionTime = 1L,
        )) {}
        compose.onNodeWithText("分类2").assertIsSelected()
    }

    @Test
    fun categoryGridScrollsWhileAmountAndKeypadRemainAvailable() {
        showSheet {}
        compose.onNodeWithTag("recordCategoryGrid").performScrollToNode(hasText("分类39"))
        compose.onNodeWithText("分类39").performClick()
        compose.onNodeWithText("0.00").assertIsDisplayed()
        compose.onNodeWithText("完成").assertIsDisplayed()
        compose.onNodeWithContentDescription("删除最后一位").assertIsDisplayed()
    }

    @Test
    fun numericKeyboardCalculatesExactCentsAndSavesEnteredNote() {
        var saved: Transaction? = null
        showSheet { saved = it }
        compose.onNodeWithText("分类0").assertIsSelected()
        compose.onNodeWithContentDescription("选择账户").performClick()
        compose.onNodeWithText("测试账户").performClick()
        compose.onNodeWithText("0").performClick()
        compose.onNodeWithTag("keypad_dot").performClick()
        compose.onNodeWithText("1").performClick()
        compose.onNodeWithTag("keypad_add").performClick()
        compose.onNodeWithText("0").performClick()
        compose.onNodeWithTag("keypad_dot").performClick()
        compose.onNodeWithText("2").performClick()
        compose.onNodeWithText("=").performClick()
        compose.onNodeWithText("0.3").assertIsDisplayed()
        compose.onNodeWithContentDescription("备注").performTextReplacement("早餐")
        compose.onNodeWithContentDescription("备注").performImeAction()
        compose.onNodeWithText("完成").performClick()
        assertEquals(30L, saved?.amount)
        assertEquals("早餐", saved?.note)
        assertEquals(1L, saved?.accountId)
        assertEquals(1L, saved?.categoryId)
    }

    @Test
    fun tallerAccountDrawerLeavesEntryHeightUnchanged() {
        showSheet {}
        val before = compose.onNodeWithTag("recordDrawer").getUnclippedBoundsInRoot()
        compose.onNodeWithContentDescription("选择账户").performClick()
        val picker = compose.onNodeWithTag("recordAccountDrawer").getUnclippedBoundsInRoot()
        assertEquals((before.bottom - before.top).value * 0.65f, (picker.bottom - picker.top).value, 0.5f)
        compose.onNodeWithContentDescription("关闭账户选择").assertIsDisplayed()
        compose.onNodeWithText("测试账户").performClick()
        assertEquals(before, compose.onNodeWithTag("recordDrawer").getUnclippedBoundsInRoot())
        compose.onNodeWithText("测试账户").assertIsDisplayed()
    }

    @Test
    fun timeUsesDialogAndTransferHasAccountSelectorsInsteadOfCategoryGrid() {
        showSheet {}
        val before = compose.onNodeWithTag("recordDrawer").getUnclippedBoundsInRoot()
        compose.onNodeWithContentDescription("选择时间").performClick()
        compose.onNodeWithTag("recordTimeDialog").assertIsDisplayed()
        compose.onNodeWithText("24 小时制").assertDoesNotExist()
        compose.onNodeWithContentDescription("小时").performScrollToIndex(23)
        compose.onNodeWithContentDescription("分钟").performScrollToIndex(59)
        compose.onNodeWithContentDescription("确认").performClick()
        compose.onNodeWithText("23:59", substring = true).assertIsDisplayed()
        assertEquals(before, compose.onNodeWithTag("recordDrawer").getUnclippedBoundsInRoot())
        compose.onNodeWithText("转账").performClick()
        compose.onNodeWithContentDescription("选择转出账户").assertIsDisplayed()
        compose.onNodeWithContentDescription("选择转入账户").assertIsDisplayed()
        compose.onNodeWithTag("recordCategoryGrid").assertDoesNotExist()
        compose.onNodeWithText("转账分类").assertDoesNotExist()
        assertEquals(before, compose.onNodeWithTag("recordDrawer").getUnclippedBoundsInRoot())
    }

    @Test
    fun transferUsesHorizontalAccountSelectorsAndSavesSwappedDirection() {
        var saved: Transaction? = null
        showSheet { saved = it }
        compose.onNodeWithText("转账").performClick()
        val source = compose.onNodeWithContentDescription("选择转出账户")
        val destination = compose.onNodeWithContentDescription("选择转入账户")
        val sourceBounds = source.getUnclippedBoundsInRoot()
        val destinationBounds = destination.getUnclippedBoundsInRoot()
        assertTrue(sourceBounds.right < destinationBounds.left)
        assertEquals(sourceBounds.top, destinationBounds.top)

        source.performClick()
        compose.onNodeWithText("测试账户").performClick()
        destination.performClick()
        compose.onNodeWithText("储蓄账户").performClick()
        compose.onNodeWithTag("keypad_5").performClick()
        compose.onNodeWithContentDescription("交换转出转入账户").performClick()
        compose.onNodeWithText("完成").performClick()
        assertEquals(TransactionType.TRANSFER, saved?.type)
        assertEquals(500L, saved?.amount)
        assertEquals(2L, saved?.accountId)
        assertEquals(1L, saved?.toAccountId)
        assertEquals(100L, saved?.categoryId)
    }

    @Test
    fun fixedHeightDrawerExpandsAboveBottomInsteadOfAtScreenTop() {
        showSheet(sheetHeight = 400.dp) {}
        val bounds = compose.onNodeWithTag("recordDrawer").getUnclippedBoundsInRoot()
        assertEquals(400.dp, bounds.bottom - bounds.top)
        assertTrue("Expanded drawer must leave space above it", bounds.top > 0.dp)
        compose.onNodeWithContentDescription("选择账户").performClick()
        val accountBounds = compose.onNodeWithTag("recordAccountDrawer").getUnclippedBoundsInRoot()
        assertEquals(260f, (accountBounds.bottom - accountBounds.top).value, 0.5f)
        assertTrue("Account drawer must open below the entry drawer top", accountBounds.top > bounds.top)
    }

    @Test
    fun completionKeySharesRightColumnAndBottomRowEdges() {
        showSheet {}
        val complete = compose.onNodeWithTag("keypad_action").getUnclippedBoundsInRoot()
        val subtract = compose.onNodeWithTag("keypad_subtract").getUnclippedBoundsInRoot()
        val zero = compose.onNodeWithTag("keypad_0").getUnclippedBoundsInRoot()
        val clear = compose.onNodeWithTag("keypad_clear").getUnclippedBoundsInRoot()
        val backspace = compose.onNodeWithTag("keypad_backspace").getUnclippedBoundsInRoot()
        assertEquals(subtract.left, complete.left)
        assertEquals(subtract.right, complete.right)
        assertEquals(zero.top, complete.top)
        assertEquals(clear.bottom, complete.bottom)
        assertEquals(subtract.bottom - subtract.top, complete.bottom - complete.top)
        assertEquals(complete.left, backspace.left)
        assertEquals(complete.right, backspace.right)
    }

    @Test
    fun dateAndTimeShareDialogWithActionsOnOppositeSides() {
        showSheet {}
        compose.onNodeWithContentDescription("选择时间").performClick()
        compose.onNodeWithContentDescription("上个月").assertDoesNotExist()
        compose.onNodeWithContentDescription("下个月").assertDoesNotExist()
        compose.onNodeWithContentDescription("选择年份").assertIsDisplayed()
        compose.onNodeWithContentDescription("选择月份").assertIsDisplayed()
        compose.onNodeWithContentDescription("小时").assertIsDisplayed()
        compose.onNodeWithContentDescription("分钟").assertIsDisplayed()
        val cancel = compose.onNodeWithContentDescription("取消").getUnclippedBoundsInRoot()
        val confirm = compose.onNodeWithContentDescription("确认").getUnclippedBoundsInRoot()
        assertTrue(cancel.right < confirm.left)
        compose.onNodeWithContentDescription("取消").performClick()
        compose.onNodeWithTag("recordTimeDialog").assertDoesNotExist()
    }

    @Test
    fun yearAndMonthCanBeSelectedWithoutRepeatedArrowPresses() {
        showSheet {}
        compose.onNodeWithContentDescription("选择时间").performClick()
        compose.onNodeWithContentDescription("选择年份").performClick()
        compose.onNodeWithTag("recordYearGrid").performScrollToNode(hasText("2024"))
        compose.onNodeWithContentDescription("年份2024").performClick()
        compose.onNodeWithContentDescription("月份02").performClick()
        compose.onNodeWithContentDescription("2024-02-29").performClick()
        compose.onNodeWithContentDescription("确认").performClick()
        compose.onNodeWithText("02.29", substring = true).assertIsDisplayed()
    }

    @Test
    fun backspaceDeletesOneDigitAndAcClearsOnlyCalculation() {
        var saved: Transaction? = null
        showSheet { saved = it }
        compose.onNodeWithText("分类0").performClick()
        compose.onNodeWithContentDescription("选择账户").performClick()
        compose.onNodeWithText("测试账户").performClick()
        compose.onNodeWithContentDescription("备注").performTextReplacement("保留备注")
        compose.onNodeWithContentDescription("备注").performImeAction()
        compose.onNodeWithText("1").performClick()
        compose.onNodeWithText("2").performClick()
        compose.onNodeWithText("3").performClick()
        compose.onNodeWithContentDescription("删除最后一位").performClick()
        compose.onNodeWithText("12").assertIsDisplayed()
        compose.onNodeWithText("AC").performClick()
        compose.onNodeWithText("0.00").assertIsDisplayed()
        compose.onNodeWithText("4").performClick()
        compose.onNodeWithText("完成").performClick()
        assertEquals(400L, saved?.amount)
        assertEquals("保留备注", saved?.note)
        assertEquals(1L, saved?.accountId)
        assertEquals(1L, saved?.categoryId)
    }

    private fun showSheet(sheetHeight: Dp = 520.dp, initialTransaction: Transaction? = null, onSave: (Transaction) -> Unit) {
        compose.setContent {
            MaterialTheme {
                RecordTransactionSheet(
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    sheetHeight = sheetHeight, initialTransaction = initialTransaction,
                    categories = categories, accounts = accounts, onDismissRequest = {},
                    onConfirm = { type, amount, accountId, destinationId, categoryId, note, time ->
                        onSave(Transaction(
                            type = type, amount = amount, accountId = accountId, toAccountId = destinationId,
                            categoryId = categoryId, note = note, transactionTime = time,
                        ))
                    },
                )
            }
        }
    }
}
