package cn.turingzy.ledger.ui.feature.wallet

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.ui.interaction.LocalUiInteractionClock
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WalletMigrationTest {
    @get:Rule val compose = createComposeRule()
    private val account = Account(42L, "测试账户", AccountType.DEBIT, currentBalance = 12345L)
    private val state = WalletUiState.Success(12345L, 0L, listOf(AccountWithTemplate(account, null)))

    @Test fun bothDeckModesOpenTheOriginalAccount() {
        val selections = mutableListOf<Account>()
        var now = 0L
        compose.setContent { CompositionLocalProvider(LocalUiInteractionClock provides { now }) {
            LedgerTheme { WalletContent(state, {}, { selections += it }) }
        } }
        compose.onNodeWithContentDescription("测试账户").performClick()
        compose.runOnIdle { assertEquals(listOf(account), selections); now = 400L }
        compose.onNodeWithContentDescription("切换为纵向层叠视图").performClick()
        compose.onNodeWithContentDescription("测试账户").performClick()
        compose.runOnIdle { assertEquals(listOf(account, account), selections); now = 800L }
        compose.onNodeWithContentDescription("切换为卡片堆叠视图").performClick()
        compose.onNodeWithContentDescription("测试账户").performClick()
        compose.runOnIdle { assertEquals(listOf(account, account, account), selections) }
    }

    @Test fun emptyDeckRemainsEmptyInBothModesAndAddIsDebounced() {
        var now = 0L
        var additions = 0
        compose.setContent {
            CompositionLocalProvider(LocalUiInteractionClock provides { now }) {
                LedgerTheme { WalletContent(state.copy(accounts = emptyList()), { additions++ }, {}) }
            }
        }
        compose.onNodeWithText("暂无账户").assertIsDisplayed()
        compose.onNodeWithContentDescription("切换为纵向层叠视图").performClick()
        compose.onNodeWithText("暂无账户").assertIsDisplayed()
        compose.onNodeWithContentDescription("添加账户").performClick().performClick()
        compose.runOnIdle { assertEquals(1, additions); now = 400L }
        compose.onNodeWithContentDescription("添加账户").performClick()
        compose.runOnIdle { assertEquals(2, additions) }
    }

    @Test fun missingRatesKeepAccountsAccessibleWithoutShowingAnAssetTotal() {
        compose.setContent { LedgerTheme { WalletContent(state.copy(amountsAvailable = false), {}, {}) } }
        compose.onNodeWithText("总资产").assertDoesNotExist()
        compose.onNodeWithContentDescription("测试账户").assertHasClickAction()
    }
}
