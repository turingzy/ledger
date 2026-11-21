package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.interaction.LocalUiInteractionClock
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsMigrationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun menuOrderAndSeparateDebouncePreserveFirstReleaseDestinations() {
        val events = mutableListOf<String>()
        var now = 0L
        val labels = listOf("常规", "卡面管理", "账单解析规则", "账单导入与导出", "关于")
        compose.setContent {
            CompositionLocalProvider(LocalUiInteractionClock provides { now }) {
                LedgerTheme {
                    SettingsContent(
                        SettingsUiState.Success(categories = emptyList()),
                        onGeneralSettingsClick = { events += labels[0] },
                        onCardManagementClick = { events += labels[1] },
                        onParseRulesClick = { events += labels[2] },
                        onBillTransferClick = { events += labels[3] },
                        onAboutClick = { events += labels[4] },
                    )
                }
            }
        }
        compose.onNodeWithText("备份与恢复").assertDoesNotExist()
        val positions = labels.map { label ->
            compose.onNodeWithText(label).getUnclippedBoundsInRoot().top
        }
        assertEquals(positions.sorted(), positions)
        labels.forEach { label -> compose.onNodeWithText(label).performScrollTo().performClick().performClick() }
        compose.runOnIdle { assertEquals(labels, events); now = 400L }
        compose.onNodeWithText("关于").performClick()
        compose.runOnIdle { assertEquals(labels + "关于", events) }
    }

    @Test fun sharedRowRetainsHeightTextOffsetAndTypography() {
        compose.setContent { LedgerTheme {
            SettingsRowItem(MaterialSymbols.Info, LedgerStyle.colors.info, "关于", {}, Modifier.testTag("settingsRow"))
        } }
        val row = compose.onNodeWithTag("settingsRow")
        row.assertHeightIsEqualTo(48.dp)
        val title = compose.onNodeWithText("关于", useUnmergedTree = true)
        assertEquals(62.dp, title.getUnclippedBoundsInRoot().left - row.getUnclippedBoundsInRoot().left)
        val results = mutableListOf<TextLayoutResult>()
        title.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(17.sp, results.single().layoutInput.style.fontSize)
        assertEquals(FontWeight.Normal, results.single().layoutInput.style.fontWeight)
    }

    @Test fun loadingDoesNotExposeMenuActions() {
        compose.setContent { LedgerTheme { SettingsContent(SettingsUiState.Loading, {}, {}, {}, {}) } }
        compose.onNodeWithText("常规").assertDoesNotExist()
        compose.onNodeWithText("关于").assertDoesNotExist()
    }
}
