package cn.turingzy.ledger.ui.feature.bill.component

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.transaction.TransactionBatchChange
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class BillActionPickerTest {
    @get:Rule val compose = createComposeRule()

    @Test fun accountSelectionOnlyAppliesAfterConfirmation() {
        verifyConfirmation(BillActionPicker.Account, "现金", TransactionBatchChange.Account(1L))
    }

    @Test fun categorySelectionOnlyAppliesAfterConfirmation() {
        verifyConfirmation(BillActionPicker.Category, "餐饮", TransactionBatchChange.Category(2L))
    }

    @Test fun categoryTabsFilterOptionsAndClearThePreviousChoice() {
        showPicker(BillActionPicker.Category, includeIncome = true)
        val confirm = compose.onNodeWithContentDescription("确认")

        compose.onNodeWithText("餐饮").assertIsDisplayed().performClick()
        confirm.assertIsEnabled()
        compose.onNodeWithText("收入").performClick()
        compose.onNodeWithText("餐饮").assertDoesNotExist()
        compose.onNodeWithText("工资").assertIsDisplayed()
        confirm.assertIsNotEnabled()

        compose.onNodeWithText("转账").assertDoesNotExist()
        compose.onNodeWithText("支出").performClick()
        compose.onNodeWithText("工资").assertDoesNotExist()
        compose.onNodeWithText("餐饮").assertIsDisplayed()
        confirm.assertIsNotEnabled()
    }

    private fun verifyConfirmation(picker: BillActionPicker, optionName: String, expected: TransactionBatchChange) {
        var applied: TransactionBatchChange? = null
        showPicker(picker) { applied = it }
        val confirm = compose.onNodeWithContentDescription("确认")
        confirm.assertIsNotEnabled()
        if (picker == BillActionPicker.Category) compose.onNodeWithText("1 / 1 笔").assertIsDisplayed()
        compose.onNodeWithText(optionName).performClick()
        compose.runOnIdle { assertNull(applied) }
        confirm.assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(expected, applied) }
    }

    private fun showPicker(
        picker: BillActionPicker,
        includeIncome: Boolean = false,
        onSelect: (TransactionBatchChange) -> Unit = {},
    ) {
        val icon = Icon(id = 1L, keyName = "food", displayName = "餐饮图标", iconUri = "res://")
        val categories = buildList {
            add(CategoryWithIcon(Category(id = 2L, name = "餐饮", type = CategoryType.EXPENSE, iconId = 1L), icon))
            if (includeIncome) add(CategoryWithIcon(Category(id = 3L, name = "工资", type = CategoryType.INCOME, iconId = 1L), icon))
        }
        compose.setContent {
            LedgerTheme {
                picker.Sheet(
                    accounts = listOf(AccountWithTemplate(Account(id = 1L, name = "现金", type = AccountType.CASH))),
                    categories = categories,
                    accountCount = { 1 },
                    categoryCount = { 1 },
                    selectedCount = categories.size,
                    type = TransactionType.EXPENSE,
                    onDismiss = {},
                    onSelect = onSelect,
                )
            }
        }
    }
}
