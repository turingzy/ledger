package cn.turingzy.ledger.ui.component.base

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ComponentsBaseTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun listRowRendersSlotsAndDoesNotBecomeClickableWithoutACallback() {
        compose.setContent {
            LedgerTheme {
                CardBase {
                    SectionHeaderBase("区块标题", subtitle = "区块说明", actions = { Text("区块操作") })
                    DividerBase()
                    ListRowBase(
                        headline = { Text("行标题") },
                        modifier = Modifier.testTag("row"),
                        supportingText = { Text("行说明") },
                        leading = { Text("前置内容") },
                        trailing = { Text("后置内容") },
                    )
                }
            }
        }
        listOf("区块标题", "区块说明", "区块操作", "行标题", "行说明", "前置内容", "后置内容").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        compose.onNodeWithTag("row").assertHasNoClickAction()
    }

    @Test
    fun clickableListRowFollowsEnabledStateOwnedByItsCaller() {
        var enabled by mutableStateOf(false)
        var clicks = 0
        compose.setContent {
            LedgerTheme {
                ListRowBase(
                    headline = { Text("操作行") },
                    modifier = Modifier.testTag("row"),
                    enabled = enabled,
                    onClick = { clicks++ },
                )
            }
        }
        compose.onNodeWithTag("row").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(0, clicks)
            enabled = true
        }
        compose.onNodeWithTag("row").assertIsEnabled().assertHasClickAction().performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun emptyStateDisplaysOptionalContentAndForwardsItsAction() {
        var clicks = 0
        compose.setContent {
            LedgerTheme {
                EmptyStateBase(
                    title = "暂无内容", message = "说明文字",
                    illustration = { Text("插画位置") },
                    actions = { ButtonBase("重试", { clicks++ }) },
                )
            }
        }
        listOf("暂无内容", "说明文字", "插画位置").forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        compose.onNodeWithText("重试").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun buttonVariantsAndIconButtonIgnoreDisabledTaps() {
        var enabled by mutableStateOf(false)
        var clicks = 0
        compose.setContent {
            LedgerTheme {
                Column {
                    ButtonVariantBase.entries.forEach { variant ->
                        ButtonBase(
                            variant.name, { clicks++ }, Modifier.testTag(variant.name),
                            enabled = enabled, variant = variant, leadingIcon = MaterialSymbols.Add,
                        )
                    }
                    IconButtonBase(
                        MaterialSymbols.Add, "新增", { clicks++ }, Modifier.testTag("icon"), enabled = enabled,
                    )
                }
            }
        }
        val tags = ButtonVariantBase.entries.map { it.name } + "icon"
        tags.forEach { compose.onNodeWithTag(it).assertIsNotEnabled().performTouchInput { click() } }
        compose.runOnIdle {
            assertEquals(0, clicks)
            enabled = true
        }
        tags.forEach { compose.onNodeWithTag(it).assertIsEnabled().performClick() }
        compose.runOnIdle { assertEquals(tags.size, clicks) }
    }
}
