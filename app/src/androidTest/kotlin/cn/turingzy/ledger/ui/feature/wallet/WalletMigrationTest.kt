package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.ui.feature.wallet.sheet.AccountDetailsContent
import cn.turingzy.ledger.ui.interaction.LocalUiInteractionClock
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WalletMigrationTest {
    @get:Rule val compose = createComposeRule()
    private val account = Account(42L, "测试账户", AccountType.DEBIT, currentBalance = 12345L)
    private val state = WalletUiState.Success(12345L, 0L, listOf(AccountWithTemplate(account, null)))

    @Test fun horizontalDeckOpensTheOriginalAccount() {
        val selections = mutableListOf<Account>()
        compose.setContent {
            LedgerTheme { WalletContent(state, {}, { selections += it }) }
        }
        compose.onNodeWithContentDescription("测试账户").performClick()
        compose.runOnIdle { assertEquals(listOf(account), selections) }
    }

    @Test fun emptyDeckShowsPlaceholderAndAddIsDebounced() {
        var now = 0L
        var additions = 0
        compose.setContent {
            CompositionLocalProvider(LocalUiInteractionClock provides { now }) {
                LedgerTheme { WalletContent(state.copy(accounts = emptyList()), { additions++ }, {}) }
            }
        }
        compose.onNodeWithText("暂无账户").assertIsDisplayed()
        val addAccount = compose.onNodeWithContentDescription("添加账户").assertHasClickAction()
        addAccount.performClick().performClick()
        compose.runOnIdle { assertEquals(1, additions); now = 400L }
        addAccount.performClick()
        compose.runOnIdle { assertEquals(2, additions) }
    }

    @Test fun missingRatesKeepAccountsAccessibleWithoutShowingAnAssetTotal() {
        compose.setContent { LedgerTheme { WalletContent(state.copy(amountsAvailable = false), {}, {}) } }
        compose.onNodeWithText("总资产").assertDoesNotExist()
        compose.onNodeWithContentDescription("测试账户").assertHasClickAction()
    }

    @Test fun accountTotalParticipationIsSavedFromTheYesNoSelector() {
        val cashAccount = account.copy(type = AccountType.CASH, includeInTotal = true)
        var saved: Account? = null
        compose.setContent {
            LedgerTheme {
                AccountDetailsContent(cashAccount, onSave = { saved = it }, onDelete = {})
            }
        }
        compose.onNodeWithContentDescription("计入总资产").performClick()
        compose.onNodeWithText("否").performClick()
        compose.onNodeWithText("否").assertIsDisplayed()
        compose.onNodeWithContentDescription("保存账户").performClick()
        compose.runOnIdle { assertEquals(cashAccount.copy(includeInTotal = false), saved) }
    }
}
