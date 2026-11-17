package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.GeneralPreferencesForm
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.AppPreferences
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class GeneralSettingsMigrationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun preferenceSelectionOnlyEmitsDraftAndDoesNotSaveImplicitly() {
        var changed: AppPreferences? = null
        var saves = 0
        compose.setContent { Content(onPreferencesChange = { changed = it }, onSave = { saves++ }, canSave = true) }
        compose.onNodeWithContentDescription("语言").performClick()
        compose.onNode(hasText("English") and hasAnyAncestor(hasTestTag("dropdownOptions"))).performClick()
        compose.onNodeWithTag("dropdownOptions").assertDoesNotExist()
        compose.runOnIdle { assertEquals("en", changed?.language); assertEquals(0, saves) }
        compose.onNodeWithContentDescription("保存").performClick()
        compose.runOnIdle { assertEquals(1, saves) }
    }

    @Test fun busyDisablesSelectionEditingAndSaveButKeepsBackAvailable() {
        var backs = 0
        compose.setContent { Content(busy = true, onBack = { backs++ }) }
        compose.onNodeWithContentDescription("保存").assertIsNotEnabled()
        compose.onNodeWithContentDescription("语言").assertIsNotEnabled()
        compose.onNodeWithContentDescription("1 USD, CNY").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.runOnIdle { assertEquals(1, backs) }
    }

    @Test fun rateEditPreservesTextCallbackAndDoesNotTriggerRefreshOrSave() {
        val changes = mutableListOf<String>()
        var saves = 0
        var refreshes = 0
        compose.setContent { Content(onRateChange = { changes += it }, onSave = { saves++ }, onRefresh = { refreshes++ }) }
        compose.onNodeWithContentDescription("1 USD, CNY").performScrollTo().performTextReplacement("7.25")
        compose.runOnIdle { assertEquals(listOf("7.25"), changes); assertEquals(0, saves); assertEquals(0, refreshes) }
        compose.onNodeWithText("刷新汇率").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, refreshes) }
    }

    @Test fun preferencePressReachesTheCardEdgeAndKeepsTheOuterCornerClipped() {
        var edgeY = 0
        compose.setContent { LedgerTheme {
            edgeY = with(LocalDensity.current) { 24.dp.roundToPx() }
            Box(Modifier.background(MaterialTheme.colorScheme.background).testTag("preferenceCard")) {
                GeneralPreferencesForm(AppPreferences(primaryCurrency = "CNY"), {})
            }
        } }
        val card = compose.onNodeWithTag("preferenceCard")
        val before = card.captureToImage().toPixelMap()
        compose.mainClock.autoAdvance = false
        val row = compose.onNodeWithContentDescription("语言")
        row.performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(200)
        val pressed = card.captureToImage().toPixelMap()
        assertNotEquals(before[2, edgeY], pressed[2, edgeY])
        assertEquals(before[2, 2], pressed[2, 2])
        row.performTouchInput { cancel() }
        compose.mainClock.autoAdvance = true
    }

    @Composable
    private fun Content(
        busy: Boolean = false,
        canSave: Boolean = false,
        onPreferencesChange: (AppPreferences) -> Unit = {},
        onRateChange: (String) -> Unit = {},
        onSave: () -> Unit = {},
        onRefresh: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        LedgerTheme {
            GeneralSettingsContent(
                AppPreferences(primaryCurrency = "CNY"), busy, canSave, "USD", "7.20", null, null,
                listOf("USD" to "USD", "EUR" to "EUR"), onPreferencesChange, {}, onRateChange, onSave, onRefresh, onBack,
            )
        }
    }
}
