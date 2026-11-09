package cn.turingzy.ledger.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsScreen
import cn.turingzy.ledger.ui.feature.ledger.LedgerScreen
import cn.turingzy.ledger.ui.feature.settings.SettingsScreen
import cn.turingzy.ledger.ui.feature.wallet.WalletScreen

@Composable
fun LedgerNavHost(
    navController: NavHostController,
    showRecordSheet: Boolean,
    onRecordSheetDismiss: () -> Unit,
    onNavigateToRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Route.Ledger,
        enterTransition = { fadeIn(tween(180)) },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(180)) },
        popExitTransition = { fadeOut(tween(120)) },
        modifier = modifier,
    ) {
        composable<Route.Ledger> {
            LedgerScreen(
                showRecordSheet = showRecordSheet,
                onRecordSheetDismiss = onRecordSheetDismiss,
                onNavigateToRecord = onNavigateToRecord,
            )
        }
        composable<Route.Analytics> {
            AnalyticsScreen()
        }
        composable<Route.Wallet> {
            WalletScreen()
        }
        composable<Route.Settings> {
            SettingsScreen()
        }
    }
}
