package cn.turingzy.ledger.ui.feature.analytics

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.ui.interaction.LocalUiInteractionClock
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AnalyticsMigrationTest {
    @get:Rule val compose = createComposeRule()
    private val state = AnalyticsUiState.Success(
        AnalyticsTimeScope.MONTH, CategoryType.EXPENSE, "2026年11月", 12345L, 0L, -12345L,
        emptyList(), LocalDate.of(2026, 11, 1),
    )

    @Test fun sharedCardAndAmountKeepTheApprovedTypography() {
        compose.setContent { LedgerTheme {
            AnalyticsContent(state, {}, {}, dashboard = AnalyticsDashboardState(loading = false))
        } }
        val title = layoutOf("分析")
        val period = layoutOf("2026年11月")
        val card = layoutOf("收支统计")
        assertEquals(28.sp, title.layoutInput.style.fontSize)
        assertEquals(FontWeight.Bold, title.layoutInput.style.fontWeight)
        assertEquals(17.sp, period.layoutInput.style.fontSize)
        assertEquals(16.sp, card.layoutInput.style.fontSize)
        assertEquals(FontWeight.Normal, card.layoutInput.style.fontWeight)
        val amounts = mutableListOf<TextLayoutResult>()
        compose.onAllNodesWithText("¥123.45")[0].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(amounts) }
        assertEquals(14.sp, amounts.single().layoutInput.style.fontSize)
        assertEquals("tnum", amounts.single().layoutInput.style.fontFeatureSettings)
    }

    @Test fun periodArrowsDebounceIndependentlyWithoutBlockingTheOppositeDirection() {
        var now = 0L
        val events = mutableListOf<Int>()
        compose.setContent { CompositionLocalProvider(LocalUiInteractionClock provides { now }) { LedgerTheme {
            AnalyticsContent(state, {}, { events += it })
        } } }
        compose.onNodeWithContentDescription("下一周期").performClick().performClick()
        compose.onNodeWithContentDescription("上一周期").performClick()
        compose.runOnIdle { assertEquals(listOf(1, -1), events); now = 400L }
        compose.onNodeWithContentDescription("下一周期").performClick()
        compose.runOnIdle { assertEquals(listOf(1, -1, 1), events) }
    }

    private fun layoutOf(text: String): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results.single()
    }
}
