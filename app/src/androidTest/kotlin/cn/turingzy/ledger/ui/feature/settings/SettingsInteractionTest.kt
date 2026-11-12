package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso.pressBack
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.navigation.SettingsDestination
import cn.turingzy.ledger.navigation.SettingsNavigation
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class SettingsInteractionTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun detailNavigationAndBackRestoreSettingsList() {
        var detailVisible by mutableStateOf(false)
        compose.setContent {
            LedgerTheme {
                SettingsNavigation(
                    onDetailVisibleChange = { detailVisible = it },
                    modifier = Modifier.fillMaxSize().padding(bottom = if (detailVisible) 0.dp else 76.dp),
                ) { destination, navigate, back ->
                    if (destination == SettingsDestination.ROOT) {
                        SettingsContent(
                            uiState = SettingsUiState.Success(categories = emptyList()),
                            onParseRulesClick = { navigate(SettingsDestination.PARSE_RULES) },
                            onCardManagementClick = { navigate(SettingsDestination.CARDS) },
                            onBillTransferClick = { navigate(SettingsDestination.BILL_TRANSFER) },
                            onBackupClick = { navigate(SettingsDestination.BACKUP) },
                            onAboutClick = { navigate(SettingsDestination.ABOUT) },
                        )
                    } else if (destination == SettingsDestination.OPEN_SOURCE_LICENSES) {
                        OpenSourceLibrariesScreen(onBack = back)
                    } else {
                        AboutScreen(
                            versionName = "1.0.0",
                            onDismissRequest = back,
                            onOpenSourceClick = { navigate(SettingsDestination.OPEN_SOURCE_LICENSES) },
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("关于").performClick()
        compose.onNodeWithText("1.0.0").assertIsDisplayed()
        compose.runOnIdle { assertEquals(true, detailVisible) }
        compose.onNodeWithText("开源许可证").performClick()
        compose.onNodeWithText("Kotlin").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("1.0.0").assertIsDisplayed()
        val buttonTop = compose.onNodeWithContentDescription("返回").fetchSemanticsNode().boundsInRoot.top
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("返回").performClick()
        compose.mainClock.advanceTimeBy(80)
        compose.runOnIdle { assertEquals(true, detailVisible) }
        assertEquals(buttonTop, compose.onNodeWithContentDescription("返回").fetchSemanticsNode().boundsInRoot.top, 0.5f)
        compose.mainClock.advanceTimeBy(400)
        compose.mainClock.autoAdvance = true
        compose.onNodeWithText("卡面管理").assertIsDisplayed()
        compose.runOnIdle { assertEquals(false, detailVisible) }
    }

    @Test
    fun ruleChoiceReturnsToEditorBeforeLeavingRules() {
        var dismissed = false
        compose.setContent {
            LedgerTheme {
                ParseRulesScreen(
                    accounts = emptyList(), categories = emptyList(), rules = emptyList(),
                    onSaveRule = {}, onDeleteRule = {}, onDismissRequest = { dismissed = true },
                )
            }
        }
        compose.onNodeWithContentDescription("新增解析规则").performClick()
        compose.onNodeWithText("默认归属").performClick()
        compose.onNodeWithText("默认账户").performClick()
        compose.onNodeWithText("导入时选择").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("新增规则").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("暂无解析规则，点击右上角添加").assertIsDisplayed()
        compose.runOnIdle { assertEquals(false, dismissed) }
        pressBack()
        compose.runOnIdle { assertEquals(true, dismissed) }
    }

    @Test
    fun addingCardFaceOpensASecondPageAndCancelKeepsTheExistingFace() {
        compose.setContent {
            LedgerTheme {
                CardTemplateManagementScreen(
                    templates = listOf(CardTemplate(id = 1L, name = "原有卡面", coverUri = "res://card")),
                    onDismissRequest = {},
                )
            }
        }
        compose.onNodeWithContentDescription("新增卡面").performClick()
        compose.onNodeWithText("新增").assertIsDisplayed()
        val titleTop = compose.onNodeWithText("新增").fetchSemanticsNode().boundsInRoot.top
        compose.onNodeWithContentDescription("卡面名称").performClick().performTextInput("新卡面")
        compose.onNodeWithContentDescription("卡面名称").assertTextEquals("新卡面")
        compose.onAllNodesWithText("新卡面").assertCountEquals(2)
        assertEquals(titleTop, compose.onNodeWithText("新增").fetchSemanticsNode().boundsInRoot.top, 0.5f)
        compose.onNodeWithContentDescription("更多颜色").performScrollTo().performClick()
        compose.onNodeWithText("自定义颜色").assertIsDisplayed()
        compose.onNodeWithContentDescription("方形色板").assertIsDisplayed()
        compose.onNodeWithContentDescription("HEX 颜色值").assertIsDisplayed()
        compose.onNodeWithContentDescription("取消颜色选择").performClick()
        compose.onNodeWithContentDescription("确认新增卡面").assertIsDisplayed()
        compose.onNodeWithText("文字颜色").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithContentDescription("卡面名称").assertTextEquals("原有卡面")
        compose.onNodeWithContentDescription("更换卡面").assertIsDisplayed()
        compose.onAllNodesWithText("新卡面").assertCountEquals(0)
        compose.onNodeWithContentDescription("删除卡面").performClick()
        compose.onNodeWithText("暂无卡面").assertIsDisplayed()
    }

    @Test
    fun compactRowShowsBackgroundAfterQuickTapInsideScrollableList() {
        var clicks = 0
        compose.setContent {
            LedgerTheme {
                Column(Modifier.background(MaterialTheme.colorScheme.surface).verticalScroll(rememberScrollState())) {
                    SettingsRowItem(
                        icon = MaterialSymbols.Info,
                        iconColor = MaterialTheme.colorScheme.primary,
                        title = "关于",
                        onClick = { clicks++ },
                        modifier = Modifier.testTag("row"),
                    )
                }
            }
        }
        val target = compose.onNodeWithTag("row")
        target.assertHeightIsEqualTo(48.dp)
        val before = target.captureToImage().toPixelMap()
        compose.mainClock.autoAdvance = false
        target.performTouchInput { down(center); up() }
        compose.mainClock.advanceTimeBy(80)
        val feedback = target.captureToImage().toPixelMap()
        assertNotEquals(before[2, 2], feedback[2, 2])
        compose.runOnIdle { assertEquals(1, clicks) }
        compose.mainClock.advanceTimeBy(400)
        val released = target.captureToImage().toPixelMap()
        assertEquals(before[2, 2], released[2, 2])
        compose.mainClock.autoAdvance = true
    }
}
