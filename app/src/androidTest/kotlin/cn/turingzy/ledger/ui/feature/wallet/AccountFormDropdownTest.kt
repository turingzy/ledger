package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.ui.component.LedgerFormDropdown as AccountFormDropdown
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import cn.turingzy.ledger.ui.theme.LedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class AccountFormDropdownTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun largeMenuOnlyBuildsVisibleOptionsAndCanSelectTheLastOne() {
        val options = (0 until 1000).map { "C${it.toString().padStart(4, '0')}" }
        val labelCalls = AtomicInteger()
        var selected: String? = null
        compose.setContent {
            LedgerTheme {
                AccountFormDropdown(
                    label = "币种", value = options.first(), options = options,
                    optionLabel = { labelCalls.incrementAndGet(); it },
                    onSelect = { selected = it },
                )
            }
        }
        compose.onNodeWithContentDescription("币种").performClick()
        compose.onNode(hasText(options.first()) and hasAnyAncestor(hasTestTag("accountDropdownOptions"))).assertIsDisplayed()
        compose.runOnIdle { assertTrue(labelCalls.get() < 100) }
        compose.onNodeWithTag("accountDropdownOptions").performScrollToIndex(options.lastIndex)
        compose.onNodeWithText(options.last()).performClick()
        compose.runOnIdle { assertEquals(options.last(), selected) }
    }
}
