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
import cn.turingzy.ledger.ui.feature.analytics.calendar.ConsumptionCalendarScreen
import java.time.YearMonth
import java.time.LocalDate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import cn.turingzy.ledger.ui.feature.bill.sheet.CreateTransactionSheet
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsScreen
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsScreen
import cn.turingzy.ledger.ui.feature.bill.BillScreen
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
    onBillSelectionVisibleChange: (Boolean) -> Unit = {},
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
                TopLevelDestination.BILL -> composable<Route.Bill> {
                    BillScreen(
                        showRecordSheet = showRecordSheet,
                        onRecordSheetDismiss = onRecordSheetDismiss,
                        onNavigateToRecord = onNavigateToRecord,
                        onSelectionVisibleChange = onBillSelectionVisibleChange,
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
                        val transactionViewModel: TransactionDetailsViewModel = hiltViewModel()
                        val snackbar = remember { SnackbarHostState() }
                        val scope = rememberCoroutineScope()
                        var recordDate by rememberSaveable { mutableStateOf<Long?>(null) }
                        ConsumptionCalendarScreen(
                            YearMonth.parse(route.month), { navController.popBackStack() },
                            onRecordTransaction = { recordDate = it.toEpochDay() },
                            onTransactionClick = transactionViewModel::selectTransaction,
                            snackbarHostState = snackbar,
                        )
                        if (recordDate == null) TransactionDetailsScreen(
                            onLoadFailure = { message -> scope.launch { snackbar.showSnackbar(message) } },
                            viewModel = transactionViewModel,
                        )
                        recordDate?.let { epochDay ->
                            CreateTransactionSheet(LocalDate.ofEpochDay(epochDay), onDismiss = { recordDate = null })
                        }
                    }
                }
                TopLevelDestination.WALLET -> composable<Route.Wallet> { WalletScreen() }
                TopLevelDestination.SETTINGS -> composable<Route.Settings> { SettingsScreen(onDetailVisibleChange = onSettingsDetailVisibleChange) }
            }
        }
    }
}
