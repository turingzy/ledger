package cn.turingzy.ledger.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import cn.turingzy.ledger.ui.feature.analytics.ConsumptionCalendarScreen
import java.time.YearMonth
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
    onAnalyticsDetailVisibleChange: (Boolean) -> Unit = {},
) {
    RetainedDestinationHost(currentDestination, modifier) { destination ->
        val navController = rememberNavController()
        val entry by navController.currentBackStackEntryAsState()
        val visibleEntries by navController.visibleEntries.collectAsStateWithLifecycle()
        if (destination == TopLevelDestination.ANALYTICS) {
            val detailVisible = (visibleEntries + listOfNotNull(entry)).any { it.destination.hasRoute<Route.ConsumptionCalendar>() }
            LaunchedEffect(detailVisible) { onAnalyticsDetailVisibleChange(detailVisible) }
        }
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
                TopLevelDestination.ANALYTICS -> {
                    composable<Route.Analytics>(
                        exitTransition = { slideOutHorizontally(tween(240)) { -it / 4 } },
                        popEnterTransition = { slideInHorizontally(tween(240)) { -it / 4 } },
                    ) {
                        AnalyticsScreen(onOpenCalendar = { date ->
                            navController.navigate(Route.ConsumptionCalendar(YearMonth.from(date).toString())) { launchSingleTop = true }
                        })
                    }
                    composable<Route.ConsumptionCalendar>(
                        enterTransition = { slideInHorizontally(tween(240)) { it } },
                        popExitTransition = { slideOutHorizontally(tween(240)) { it } },
                    ) { calendarEntry ->
                        val route = calendarEntry.toRoute<Route.ConsumptionCalendar>()
                        ConsumptionCalendarScreen(YearMonth.parse(route.month), { navController.popBackStack() })
                    }
                }
                TopLevelDestination.WALLET -> composable<Route.Wallet> { WalletScreen() }
                TopLevelDestination.SETTINGS -> composable<Route.Settings> { SettingsScreen(onDetailVisibleChange = onSettingsDetailVisibleChange) }
            }
        }
    }
}
