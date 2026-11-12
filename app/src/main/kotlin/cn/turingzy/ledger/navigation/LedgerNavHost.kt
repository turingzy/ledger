package cn.turingzy.ledger.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsScreen
import cn.turingzy.ledger.ui.feature.ledger.LedgerScreen
import cn.turingzy.ledger.ui.feature.settings.SettingsScreen
import cn.turingzy.ledger.ui.feature.wallet.WalletScreen

@Composable
fun LedgerNavHost(
    currentDestination: TopLevelDestination,
    showRecordSheet: Boolean,
    onRecordSheetDismiss: () -> Unit,
    onNavigateToRecord: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingsDetailVisibleChange: (Boolean) -> Unit = {},
) {
    RetainedDestinationHost(currentDestination, modifier) { destination ->
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = destination.route,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
        ) {
            when (destination) {
                TopLevelDestination.LEDGER -> composable<Route.Ledger> {
                    LedgerScreen(
                        showRecordSheet = showRecordSheet,
                        onRecordSheetDismiss = onRecordSheetDismiss,
                        onNavigateToRecord = onNavigateToRecord,
                    )
                }
                TopLevelDestination.ANALYTICS -> composable<Route.Analytics> { AnalyticsScreen() }
                TopLevelDestination.WALLET -> composable<Route.Wallet> { WalletScreen() }
                TopLevelDestination.SETTINGS -> composable<Route.Settings> { SettingsScreen(onDetailVisibleChange = onSettingsDetailVisibleChange) }
            }
        }
    }
}
