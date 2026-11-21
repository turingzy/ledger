package cn.turingzy.ledger.ui.feature.wallet.sheet

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccountDetailsDeleteTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun accountDetailsShowsTheAssignedCardFaceOutsideTheSelectableList() {
        val assignedTemplate = CardTemplate(id = 42L, name = "已选卡面", coverUri = "", isDeleted = true)
        compose.setContent {
            LedgerTheme {
                AccountDetailsContent(
                    account = Account(id = 7L, name = "银行卡", type = AccountType.DEBIT,
                        cardNumber = "1234", templateId = assignedTemplate.id),
                    onSave = {}, onDelete = {}, assignedTemplate = assignedTemplate,
                )
            }
        }
        compose.onNodeWithText("已选卡面").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun deletingRequiresConfirmationAndNeverSavesEditedFields() {
        var deletes = 0
        var saves = 0
        compose.setContent {
            LedgerTheme {
                AccountDetailsContent(
                    account = Account(id = 7L, name = "现金", type = AccountType.CASH),
                    onSave = { saves++ }, onDelete = { deletes++ },
                )
            }
        }
        compose.onNodeWithText("删除账户").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, deletes) }
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(0, deletes) }
        compose.onNodeWithText("删除账户").performScrollTo().performClick()
        compose.onNodeWithText("删除").performClick()
        compose.runOnIdle {
            assertEquals(1, deletes)
            assertEquals(0, saves)
        }
    }
}
