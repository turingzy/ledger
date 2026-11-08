package cn.turingzy.ledger.ui

import androidx.compose.foundation.background
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding.let { androidx.compose.foundation.layout.PaddingValues(0.dp) })
                .background(MaterialTheme.colorScheme.background),
        ) {
            // 主内容区域，底部预留 72.dp 防止被悬浮胶囊底栏遮挡，顶部由内层 Screen 自主控制状态栏间距
            LedgerNavHost(
                navController = navController,
                showRecordSheet = showGlobalRecordSheet,
                onRecordSheetDismiss = { showGlobalRecordSheet = false },
                onNavigateToRecord = { showGlobalRecordSheet = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
            )

            // 右侧悬浮“记一笔”胶囊按钮 (仅在账本Tab呈现，位于底栏上方)
            LedgerFloatingActionButton(
                visible = isLedgerTab,
                onClick = { showGlobalRecordSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 80.dp),
            )

            // 底部磨砂胶囊导航栏
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
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            )
        }
    }
}
