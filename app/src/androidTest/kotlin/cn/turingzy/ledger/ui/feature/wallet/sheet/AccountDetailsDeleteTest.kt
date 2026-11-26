package cn.turingzy.ledger.ui.feature.wallet.sheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.ui.feature.wallet.component.WalletAccountInfo
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AccountDetailsDeleteTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun accountDetailsShowsTheAssignedCardFaceAndOpensTheCardFaceDialog() {
        val assignedTemplate = CardTemplate(id = 42L, name = "已选卡面", coverUri = "", isDeleted = true)
        compose.setContent {
            LedgerTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    WalletAccountInfo(
                        entry = AccountWithTemplate(
                            Account(id = 7L, name = "银行卡", type = AccountType.DEBIT,
                                cardNumber = "1234", templateId = assignedTemplate.id),
                            assignedTemplate,
                        ),
                        templates = listOf(CardTemplate(id = 43L, name = "可选卡面", coverUri = "")),
                        onUpdate = {}, onDelete = {},
                    )
                }
            }
        }
        compose.onNodeWithText("已选卡面").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("卡面").performScrollTo().performClick()
        compose.onNodeWithText("选择卡面").assertIsDisplayed()
    }

    @Test
    fun deletingRequiresConfirmationAndNeverSavesEditedFields() {
        var deletes = 0
        var saves = 0
        compose.setContent {
            LedgerTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    WalletAccountInfo(
                        entry = AccountWithTemplate(Account(id = 7L, name = "现金", type = AccountType.CASH)),
                        templates = emptyList(), onUpdate = { saves++ }, onDelete = { deletes++ },
                    )
                }
            }
        }
        compose.onNodeWithText("删除账户").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, deletes) }
        compose.onNodeWithText("取消").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, deletes) }
        compose.onNodeWithText("删除账户").performScrollTo().performClick()
        compose.onNodeWithText("删除").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, deletes)
            assertEquals(0, saves)
        }
    }
}
