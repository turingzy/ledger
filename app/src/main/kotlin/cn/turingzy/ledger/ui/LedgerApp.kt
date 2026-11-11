package cn.turingzy.ledger.ui

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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

    val isLedgerTab = currentTopLevelDestination == TopLevelDestination.LEDGER
    var showGlobalRecordSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = !isLedgerTab) {
        currentTopLevelDestination = TopLevelDestination.LEDGER
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding.let { androidx.compose.foundation.layout.PaddingValues(0.dp) })
                .background(MaterialTheme.colorScheme.background),
        ) {
            LedgerNavHost(
                currentDestination = currentTopLevelDestination,
                showRecordSheet = showGlobalRecordSheet,
                onRecordSheetDismiss = { showGlobalRecordSheet = false },
                onNavigateToRecord = { showGlobalRecordSheet = true },
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = 76.dp),
            )

            LedgerFloatingActionButton(
                visible = isLedgerTab,
                onClick = { showGlobalRecordSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 76.dp),
            )

            LedgerBottomBar(
                destinations = TopLevelDestination.entries,
                currentDestination = currentTopLevelDestination,
                onNavigateToDestination = { targetDestination ->
                    currentTopLevelDestination = targetDestination
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}
