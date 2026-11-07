package cn.turingzy.ledger.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions
import cn.turingzy.ledger.navigation.LedgerNavHost
import cn.turingzy.ledger.navigation.Route
import cn.turingzy.ledger.navigation.TopLevelDestination
import cn.turingzy.ledger.ui.component.LedgerBottomBar
import cn.turingzy.ledger.ui.component.LedgerFloatingActionButton

@Composable
fun LedgerApp(
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val currentTopLevelDestination = TopLevelDestination.entries.firstOrNull { topLevelDest ->
        currentDestination?.hierarchy?.any { it.hasRoute(topLevelDest.route::class) } == true
    } ?: TopLevelDestination.LEDGER

    val isLedgerTab = currentTopLevelDestination == TopLevelDestination.LEDGER
    var showGlobalRecordSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            LedgerBottomBar(
                destinations = TopLevelDestination.entries,
                currentDestination = currentTopLevelDestination,
                onNavigateToDestination = { targetDestination ->
                    val navOptions = navOptions {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                    when (targetDestination.route) {
                        Route.Ledger -> navController.navigate(Route.Ledger, navOptions)
                        Route.Analytics -> navController.navigate(Route.Analytics, navOptions)
                        Route.Wallet -> navController.navigate(Route.Wallet, navOptions)
                        Route.Settings -> navController.navigate(Route.Settings, navOptions)
                    }
                },
            )
        },
        floatingActionButton = {
            LedgerFloatingActionButton(
                visible = isLedgerTab,
                onClick = {
                    showGlobalRecordSheet = true
                },
            )
        },
    ) { innerPadding ->
        LedgerNavHost(
            navController = navController,
            showRecordSheet = showGlobalRecordSheet,
            onRecordSheetDismiss = { showGlobalRecordSheet = false },
            onNavigateToRecord = {
                showGlobalRecordSheet = true
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}
