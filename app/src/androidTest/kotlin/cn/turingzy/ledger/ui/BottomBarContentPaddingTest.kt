package cn.turingzy.ledger.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.ui.component.BottomBarContentPadding
import cn.turingzy.ledger.ui.component.LocalBottomBarContentPadding
import cn.turingzy.ledger.ui.feature.analytics.*
import cn.turingzy.ledger.ui.feature.ledger.*
import cn.turingzy.ledger.ui.feature.wallet.*
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BottomBarContentPaddingTest {
    @get:Rule val compose = createComposeRule()
    private val verticalScroll = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    @Test fun ledgerViewportReachesBottomAndLastDayClearsFloatingNavigation() {
        show {
            LedgerContent(LedgerUiState.Success(2026, 11, "2026.11", "结余", 0L, 0L, 0L,
                List(30) { DayTransactionGroup(it.toLong(), "第${it}天", 0L, 0L, emptyList()) }), Modifier.testTag("page"))
        }
        assertViewportReachesBottom()
        compose.onNodeWithText("第29天").performScrollTo()
        compose.onNode(verticalScroll).performScrollToIndex(30)
        val last = compose.onNodeWithText("第29天").getUnclippedBoundsInRoot()
        val page = compose.onNodeWithTag("page").getUnclippedBoundsInRoot()
        assertTrue(page.bottom - last.bottom >= BottomBarContentPadding)
    }

    @Test fun analyticsViewportReachesBottomBehindFloatingNavigation() {
        show {
            AnalyticsContent(AnalyticsUiState.Success(AnalyticsTimeScope.MONTH, CategoryType.EXPENSE, "2026.11",
                0L, 0L, 0L, emptyList(), LocalDate.of(2026, 11, 1)), {}, {}, Modifier.testTag("page"))
        }
        assertViewportReachesBottom()
    }

    @Test fun walletViewportReachesBottomEvenWithShortContent() {
        show { WalletContent(WalletUiState.Success(0L, 0L, emptyList()), {}, {}, Modifier.testTag("page")) }
        assertViewportReachesBottom()
    }

    private fun show(content: @Composable () -> Unit) {
        compose.setContent {
            MaterialTheme { CompositionLocalProvider(LocalBottomBarContentPadding provides BottomBarContentPadding) { content() } }
        }
    }
    private fun assertViewportReachesBottom() {
        val viewport = compose.onNode(verticalScroll).getUnclippedBoundsInRoot()
        val page = compose.onNodeWithTag("page").getUnclippedBoundsInRoot()
        assertEquals(page.bottom, viewport.bottom)
    }
}
