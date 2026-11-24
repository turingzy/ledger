package cn.turingzy.ledger.ui.feature.bill.sheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.currency.CurrencyAmountOverride
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.interaction.LocalSystemClickFeedback
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
    fun foreignAmountUsesTheTransactionDateAndRefreshesWhenTheAmountChanges() {
        val time = 1_763_200_000_000L
        val dollarAccount = AccountWithTemplate(Account(
            id = 3L, name = "美元账户", type = AccountType.DEBIT, currency = "USD",
        ))
        val original = Transaction(
            id = 9L, type = TransactionType.EXPENSE, amount = 100L, accountId = 3L,
            categoryId = 1L, transactionTime = time, baseCurrency = "CNY", baseAmount = 680L,
        )
        var requestedRate: Triple<String, String, Long>? = null
        var savedOverride: CurrencyAmountOverride? = null
        compose.setContent {
            MaterialTheme {
                RecordTransactionSheet(
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    categories = categories, accounts = listOf(dollarAccount),
                    onDismissRequest = {},
                    onConfirm = { _, _, _, _, _, _, _, _, amountOverride -> savedOverride = amountOverride },
                    initialTransaction = original,
                    onLoadExchangeRate = { from, to, timestamp ->
                        requestedRate = Triple(from, to, timestamp)
                        "7.20"
                    },
                )
            }
        }
        compose.onNodeWithText("≈ ¥6.80").assertIsDisplayed()
        val timeChip = compose.onNodeWithContentDescription("选择时间").getUnclippedBoundsInRoot()
        val convertedAmountChip = compose.onNodeWithContentDescription("修改折算金额").getUnclippedBoundsInRoot()
        assertEquals(timeChip.top, convertedAmountChip.top)
        assertTrue(convertedAmountChip.left >= timeChip.right)
        compose.runOnIdle { assertEquals(null, requestedRate) }
        repeat(2) { compose.onNodeWithTag("keypad_backspace").performClick() }
        compose.onNodeWithTag("keypad_2").performClick()
        compose.onNodeWithText("≈ ¥8.64").assertIsDisplayed()
        compose.runOnIdle { assertEquals(Triple("USD", "CNY", time), requestedRate) }
        compose.onNodeWithText("≈ ¥8.64").performClick()
        compose.onNodeWithTag("primaryAmountInput").performTextReplacement("9.00")
        compose.onNodeWithText("确认").performClick()
        compose.onNodeWithText("¥9.00").assertIsDisplayed()
        compose.onNodeWithTag("keypad_action").performClick()
        compose.runOnIdle { assertEquals(CurrencyAmountOverride("CNY", 900L), savedOverride) }
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
        compose.onNodeWithContentDescription("关闭账户选择").assertDoesNotExist()
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
        compose.onNodeWithText("确认").performClick()
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
    fun cancellingDateTimeChangesKeepsTheOriginalTransactionTime() {
        val initialTime = java.time.LocalDate.of(2024, 2, 29).atTime(12, 30)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        var saved: Transaction? = null
        showSheet(initialTransactionTime = initialTime) { saved = it }
        compose.onNodeWithContentDescription("选择时间").performClick()
        compose.onNodeWithContentDescription("2024-02-28").performClick()
        compose.onNodeWithContentDescription("小时").performScrollToIndex(23)
        compose.onNodeWithContentDescription("分钟").performScrollToIndex(59)
        compose.onNodeWithText("取消").performClick()
        compose.onNodeWithText("12:30", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("keypad_1").performClick()
        compose.onNodeWithText("完成").performClick()
        compose.runOnIdle { assertEquals(initialTime, checkNotNull(saved).transactionTime) }
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
        val cancel = compose.onNodeWithText("取消").getUnclippedBoundsInRoot()
        val confirm = compose.onNodeWithText("确认").getUnclippedBoundsInRoot()
        assertTrue(cancel.right < confirm.left)
        compose.onNodeWithText("取消").performClick()
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
        compose.onNodeWithText("确认").performClick()
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

    @Test
    fun recordingFromCalendarKeepsTheSelectedDateWhenSaving() {
        val initialTime = java.time.LocalDate.of(2024, 2, 29).atTime(12, 30)
            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        var saved: Transaction? = null
        showSheet(initialTransactionTime = initialTime) { saved = it }
        compose.onNodeWithText("1").performClick()
        compose.onNodeWithText("完成").performClick()
        compose.runOnIdle { assertEquals(initialTime, checkNotNull(saved).transactionTime) }
    }

    @Test
    fun accountPickerCentersItsTitleAndClosesFromTheLeft() {
        showSheet {}
        compose.onNodeWithText("*").assertIsDisplayed()
        compose.onNodeWithContentDescription("选择账户").performClick()
        val drawer = compose.onNodeWithTag("recordAccountDrawer")
        val title = compose.onNode(hasText("选择账户") and hasAnyAncestor(hasTestTag("recordAccountDrawer")))
        val close = compose.onNodeWithContentDescription("关闭账户选择")

        val titleBounds = title.fetchSemanticsNode().boundsInRoot
        assertEquals(drawer.fetchSemanticsNode().boundsInRoot.center.x, titleBounds.center.x, 1f)
        assertTrue(close.fetchSemanticsNode().boundsInRoot.right < titleBounds.left)
        close.performClick()
        drawer.assertDoesNotExist()
    }

    @Test
    fun keypadUsesHostFeedbackForEveryTapInsideTheSheet() {
        var clicks = 0
        showSheet(onClickFeedback = { clicks++ }) {}
        compose.onNodeWithTag("keypad_5").performClick().performClick()
        compose.onNodeWithTag("keypad_backspace").performClick()
        compose.runOnIdle { assertEquals(3, clicks) }
    }

    private fun showSheet(
        sheetHeight: Dp = 520.dp,
        initialTransaction: Transaction? = null,
        initialTransactionTime: Long? = null,
        onClickFeedback: (() -> Unit)? = null,
        onSave: (Transaction) -> Unit,
    ) {
        compose.setContent {
            CompositionLocalProvider(LocalSystemClickFeedback provides onClickFeedback) {
                MaterialTheme {
                    RecordTransactionSheet(
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        sheetHeight = sheetHeight, initialTransaction = initialTransaction, initialTransactionTime = initialTransactionTime,
                        categories = categories, accounts = accounts, onDismissRequest = {},
                        onConfirm = { type, amount, accountId, destinationId, categoryId, note, time, toAmount, _ ->
                            onSave(Transaction(
                                type = type, amount = amount, accountId = accountId, toAccountId = destinationId,
                                categoryId = categoryId, note = note, transactionTime = time, toAmount = toAmount,
                            ))
                        },
                    )
                }
            }
        }
    }
}
