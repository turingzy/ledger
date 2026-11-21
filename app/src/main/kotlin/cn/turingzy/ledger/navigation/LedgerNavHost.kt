package cn.turingzy.ledger.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import cn.turingzy.ledger.ui.feature.analytics.calendar.ConsumptionCalendarScreen
import java.time.YearMonth
import java.time.LocalDate
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import cn.turingzy.ledger.ui.feature.bill.sheet.RecordCreateSheet
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsScreen
import cn.turingzy.ledger.ui.feature.bill.BillScreen
import cn.turingzy.ledger.ui.feature.bill.search.BillSearchScreen
import cn.turingzy.ledger.ui.feature.settings.SettingsScreen
import cn.turingzy.ledger.ui.feature.wallet.WalletScreen

@Composable
fun LedgerNavHost(
    currentDestination: TopLevelDestination,
    showRecordSheet: Boolean,
    onRecordSheetDismiss: () -> Unit,
    onNavigateToRecord: () -> Unit,
    modifier: Modifier = Modifier,
    onBillSelectionVisibleChange: (Boolean) -> Unit = {},
    onSubpageChange: (TopLevelDestination, Boolean) -> Unit = { _, _ -> },
) {
    RetainedDestinationHost(currentDestination, modifier) { destination ->
        val navController = rememberNavController()
        if (destination == TopLevelDestination.ANALYTICS || destination == TopLevelDestination.BILL) {
            val entry by navController.currentBackStackEntryAsState()
            val isSubpage = entry?.destination?.let { it.hasRoute<Route.BillSearch>() || it.hasRoute<Route.ConsumptionCalendar>() } == true
            LaunchedEffect(isSubpage) { onSubpageChange(destination, isSubpage) }
        }
        NavHost(
            navController = navController,
            startDestination = destination.route,
            enterTransition = { slideInHorizontally(tween(240)) { it } },
            exitTransition = { slideOutHorizontally(tween(240)) { -it / 4 } },
            popEnterTransition = { slideInHorizontally(tween(240)) { -it / 4 } },
            popExitTransition = { slideOutHorizontally(tween(240)) { it } },
        ) {
            when (destination) {
                TopLevelDestination.BILL -> {
                    composable<Route.Bill> {
                        BillScreen(
                            showRecordSheet = showRecordSheet,
                            onRecordSheetDismiss = onRecordSheetDismiss,
                            onNavigateToRecord = onNavigateToRecord,
                            onSelectionVisibleChange = onBillSelectionVisibleChange,
                            onOpenSearch = { navController.navigate(Route.BillSearch) { launchSingleTop = true } },
                        )
                    }
                    composable<Route.BillSearch> {
                        BillSearchScreen(onBack = { navController.popBackStack() })
                    }
                }
                TopLevelDestination.ANALYTICS -> {
                    composable<Route.Analytics> {
                        AnalyticsScreen(onOpenCalendar = { date ->
                            navController.navigate(Route.ConsumptionCalendar(YearMonth.from(date).toString())) { launchSingleTop = true }
                        })
                    }
                    composable<Route.ConsumptionCalendar> { calendarEntry ->
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
                            RecordCreateSheet(LocalDate.ofEpochDay(epochDay), onDismiss = { recordDate = null })
                        }
                    }
                }
                TopLevelDestination.WALLET -> composable<Route.Wallet> { WalletScreen() }
                TopLevelDestination.SETTINGS -> composable<Route.Settings> {
                    SettingsScreen(onSubpageChange = { onSubpageChange(destination, it) })
                }
            }
        }
    }
}
