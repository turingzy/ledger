package cn.turingzy.ledger.ui.feature.settings.screen

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ParseRulesMigrationTest {
    @get:Rule val compose = createComposeRule()
    private val rule = defaultParseRulePreviews().first()

    @Test fun saveKeepsAllMappingsAndOnlyTrimsTheRuleName() {
        val edited = rule.copy(name = "  更新规则  ", accountId = 7L, categoryId = 9L)
        var saved: ParseRulePreview? = null
        compose.setContent { LedgerTheme {
            ParseRulesScreen(emptyList(), emptyList(), listOf(rule), { saved = it }, {}, {}, initialDraft = edited)
        } }
        compose.onNodeWithContentDescription("保存规则").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(edited.copy(name = "更新规则"), saved) }
        compose.onNodeWithText("账单解析规则").assertIsDisplayed()
    }

    @Test fun invalidDraftCannotSaveAndBackDoesNotDiscardTheList() {
        var saved: ParseRulePreview? = null
        var dismissed = false
        compose.setContent { LedgerTheme {
            ParseRulesScreen(emptyList(), emptyList(), listOf(rule), { saved = it }, {}, { dismissed = true },
                initialDraft = rule.copy(name = ""))
        } }
        compose.onNodeWithContentDescription("保存规则").assertIsNotEnabled()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText(rule.name).assertIsDisplayed()
        compose.runOnIdle { assertNull(saved); assertEquals(false, dismissed) }
    }

    @Test fun deleteKeepsTheOriginalRuleIdAndReturnsToTheList() {
        var deleted: Long? = null
        compose.setContent { LedgerTheme {
            ParseRulesScreen(emptyList(), emptyList(), listOf(rule), {}, { deleted = it }, {}, initialDraft = rule)
        } }
        compose.onNodeWithText("删除规则").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(rule.id, deleted) }
        compose.onNodeWithText("账单解析规则").assertIsDisplayed()
    }
}
