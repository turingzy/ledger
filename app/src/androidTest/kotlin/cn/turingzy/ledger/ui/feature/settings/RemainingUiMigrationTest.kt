package cn.turingzy.ledger.ui.feature.settings

import cn.turingzy.ledger.ui.feature.settings.component.SettingsTransferAction
import cn.turingzy.ledger.ui.feature.settings.screen.ParseRulesScreen
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.ui.component.input.GeneralPreferencesForm
import cn.turingzy.ledger.ui.interaction.LocalUiInteractionClock
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RemainingUiMigrationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun preferencesIndicatorTracksExpandedAndCollapsedState() {
        compose.setContent { LedgerTheme { GeneralPreferencesForm(AppPreferences(primaryCurrency = "CNY"), {}) } }
        compose.onNodeWithContentDescription("语言").assert(hasStateDescription("已折叠")).performClick()
        compose.onNodeWithContentDescription("语言").assert(hasStateDescription("已展开"))
        compose.onNode(hasText("English") and hasAnyAncestor(hasTestTag("dropdownOptions"))).performClick()
        compose.onNodeWithContentDescription("语言").assert(hasStateDescription("已折叠"))
    }

    @Test fun ruleIndicatorAlsoTracksTheOpenMenu() {
        compose.setContent { LedgerTheme { ParseRulesScreen(emptyList(), emptyList(), emptyList(), {}, {}, {}) } }
        compose.onNodeWithContentDescription("默认账户").performScrollTo().assert(hasStateDescription("已折叠")).performClick()
        compose.onNodeWithTag("ruleAccountOptions").assertIsDisplayed()
        compose.onNodeWithContentDescription("默认账户").assert(hasStateDescription("已展开"))
    }

    @Test fun startingATransferDoesNotDebounceTheImmediateCancelAction() {
        val events = mutableListOf<String>()
        var running by mutableStateOf(false)
        compose.setContent {
            CompositionLocalProvider(LocalUiInteractionClock provides { 0L }) {
                LedgerTheme {
                    SettingsTransferAction(if (running) "取消" else "开始", running, null, {
                        events += if (running) "cancel" else "start"
                        running = !running
                    })
                }
            }
        }
        compose.onNodeWithText("开始").performClick()
        compose.onNodeWithText("取消").performClick()
        compose.runOnIdle { assertEquals(listOf("start", "cancel"), events) }
    }
}
