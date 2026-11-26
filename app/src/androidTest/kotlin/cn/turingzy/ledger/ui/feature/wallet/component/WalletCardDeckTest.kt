package cn.turingzy.ledger.ui.feature.wallet.component

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WalletCardDeckTest {
    @get:Rule val compose = createComposeRule()

    private val cash = AccountWithTemplate(Account(1L, "现金", AccountType.CASH), null)
    private val bank = AccountWithTemplate(Account(2L, "银行卡", AccountType.DEBIT), null)

    @Test fun singleAccountUsesTheFullWidthWithoutAClickAction() {
        compose.setContent {
            LedgerTheme {
                SwipeableBankCardDeck(listOf(cash), Modifier.width(280.dp))
            }
        }
        compose.onNodeWithContentDescription("现金").assertWidthIsEqualTo(280.dp).assert(!hasClickAction())
    }

    @Test fun twoAccountsReserveEdgesAndLoopInBothDirections() {
        var stacked by mutableStateOf(false)
        compose.setContent {
            LedgerTheme {
                SwipeableBankCardDeck(
                    if (stacked) listOf(cash, bank) else listOf(cash),
                    Modifier.width(280.dp).testTag("walletCardDeck"),
                )
            }
        }
        val deck = compose.onNodeWithTag("walletCardDeck")
        val singleBounds = deck.getUnclippedBoundsInRoot()
        compose.runOnIdle { stacked = true }
        val stackBounds = deck.getUnclippedBoundsInRoot()
        assertEquals(singleBounds.bottom - singleBounds.top, stackBounds.bottom - stackBounds.top)
        val cashCard = compose.onNodeWithContentDescription("现金")
        val bankCard = compose.onNodeWithContentDescription("银行卡")
        cashCard.assertWidthIsEqualTo(248.dp)
        bankCard.assertDoesNotExist()

        deck.performTouchInput { swipeLeft() }
        bankCard.assertIsDisplayed()
        cashCard.assertDoesNotExist()
        bankCard.assert(!hasClickAction())

        deck.performTouchInput { swipeLeft() }
        cashCard.assertIsDisplayed()
        deck.performTouchInput { swipeRight() }
        bankCard.assertIsDisplayed()
    }
}
