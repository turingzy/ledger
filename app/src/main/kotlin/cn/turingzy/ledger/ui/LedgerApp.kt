package cn.turingzy.ledger.ui

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import cn.turingzy.ledger.ui.component.BottomBarContentPadding
import cn.turingzy.ledger.ui.component.LocalBottomBarContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.navigation.LedgerNavHost
import cn.turingzy.ledger.navigation.TopLevelDestination
import cn.turingzy.ledger.ui.component.LedgerBottomBar
import cn.turingzy.ledger.ui.component.LedgerFloatingActionButton

@Composable
fun LedgerApp(
    modifier: Modifier = Modifier,
) {
    var currentTopLevelDestination by rememberSaveable { mutableStateOf(TopLevelDestination.LEDGER) }

    var settingsDetailVisible by remember { mutableStateOf(false) }
    val showBottomBar = currentTopLevelDestination != TopLevelDestination.SETTINGS || !settingsDetailVisible
    val isLedgerTab = currentTopLevelDestination == TopLevelDestination.LEDGER
    var showGlobalRecordSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = !isLedgerTab) {
        currentTopLevelDestination = TopLevelDestination.LEDGER
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        CompositionLocalProvider(LocalBottomBarContentPadding provides if (showBottomBar) BottomBarContentPadding else 0.dp) {
            LedgerNavHost(
                currentDestination = currentTopLevelDestination,
                showRecordSheet = showGlobalRecordSheet,
                onRecordSheetDismiss = { showGlobalRecordSheet = false },
                onNavigateToRecord = { showGlobalRecordSheet = true },
                onSettingsDetailVisibleChange = { settingsDetailVisible = it },
                modifier = Modifier.fillMaxSize()
                    .then(if (showBottomBar) Modifier else Modifier.navigationBarsPadding()),
            )
        }

        LedgerFloatingActionButton(
            visible = isLedgerTab,
            onClick = { showGlobalRecordSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = BottomBarContentPadding),
        )

        if (showBottomBar) LedgerBottomBar(
            destinations = TopLevelDestination.entries,
            currentDestination = currentTopLevelDestination,
            onNavigateToDestination = { targetDestination ->
                currentTopLevelDestination = targetDestination
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
