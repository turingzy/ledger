package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccountDetailsDeleteTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun deletingRequiresConfirmationAndNeverSavesEditedFields() {
        var deletes = 0
        var saves = 0
        compose.setContent {
            LedgerTheme {
                AccountDetailsContent(
                    account = Account(id = 7L, name = "现金", type = AccountType.CASH),
                    onClose = {}, onSave = { saves++ }, onDelete = { deletes++ },
                )
            }
        }
        compose.onNodeWithText("删除账户").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, deletes) }
        compose.onNodeWithContentDescription("取消删除账户").performClick()
        compose.runOnIdle { assertEquals(0, deletes) }
        compose.onNodeWithText("删除账户").performScrollTo().performClick()
        compose.onNodeWithContentDescription("确认删除账户").performClick()
        compose.runOnIdle {
            assertEquals(1, deletes)
            assertEquals(0, saves)
        }
    }
}
