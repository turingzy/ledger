package cn.turingzy.ledger.ui

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import cn.turingzy.ledger.ui.component.navigation.BottomBarContentPadding
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.navigation.LedgerNavHost
import cn.turingzy.ledger.navigation.TopLevelDestination
import cn.turingzy.ledger.ui.component.base.rememberBackdropStateBaseBase
import cn.turingzy.ledger.ui.component.base.recordBackdropBase
import cn.turingzy.ledger.ui.component.navigation.LedgerBottomBar
import cn.turingzy.ledger.ui.component.navigation.LedgerFloatingActionButton
import cn.turingzy.ledger.ui.interaction.LocalSystemClickFeedback
import cn.turingzy.ledger.ui.interaction.rememberSystemClickFeedback

/** Hosts the top-level bar and hides it while a subpage owns the screen. */
@Composable
fun LedgerApp(
    modifier: Modifier = Modifier,
) {
    val navigationBackdrop = rememberBackdropStateBaseBase()
    val systemClickFeedback = rememberSystemClickFeedback()
    var currentTopLevelDestination by rememberSaveable { mutableStateOf(TopLevelDestination.BILL) }

    var billSelectionVisible by remember { mutableStateOf(false) }
    val isBillTab = currentTopLevelDestination == TopLevelDestination.BILL
    val subpages = remember { mutableStateMapOf<TopLevelDestination, Boolean>() }
    val isSubpage = subpages[currentTopLevelDestination] == true
    val billSelectionActive = isBillTab && billSelectionVisible
    var showGlobalRecordSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = !isBillTab) {
        currentTopLevelDestination = TopLevelDestination.BILL
    }

    CompositionLocalProvider(
        LocalSystemClickFeedback provides systemClickFeedback,
        LocalBottomBarContentPadding provides if (billSelectionActive) 0.dp else BottomBarContentPadding,
    ) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LedgerNavHost(
                currentDestination = currentTopLevelDestination,
                showRecordSheet = showGlobalRecordSheet,
                onRecordSheetDismiss = { showGlobalRecordSheet = false },
                onNavigateToRecord = { showGlobalRecordSheet = true },
                onBillSelectionVisibleChange = { billSelectionVisible = it },
                onSubpageChange = { destination, visible -> subpages[destination] = visible },
                modifier = Modifier.fillMaxSize().recordBackdropBase(navigationBackdrop)
                    .then(if (billSelectionActive) Modifier.navigationBarsPadding() else Modifier),
            )

            LedgerFloatingActionButton(
                visible = isBillTab && !isSubpage && !billSelectionVisible,
                onClick = { showGlobalRecordSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = BottomBarContentPadding),
            )

            // Reattach the bar as the root page returns, without a separate fade-in.
            if (!isSubpage) LedgerBottomBar(
                visible = !billSelectionActive,
                destinations = TopLevelDestination.entries,
                currentDestination = currentTopLevelDestination,
                onNavigateToDestination = { currentTopLevelDestination = it },
                modifier = Modifier.align(Alignment.BottomCenter),
                backdrop = navigationBackdrop,
            )
        }
    }
}
