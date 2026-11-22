package cn.turingzy.ledger.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import androidx.compose.ui.draw.clipToBounds
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsScreen
import cn.turingzy.ledger.ui.feature.analytics.calendar.ConsumptionCalendarScreen
import cn.turingzy.ledger.ui.feature.bill.BillScreen
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsScreen
import cn.turingzy.ledger.ui.feature.bill.detail.TransactionDetailsViewModel
import cn.turingzy.ledger.ui.feature.bill.sheet.RecordCreateSheet
import cn.turingzy.ledger.ui.feature.bill.search.BillSearchScreen
import cn.turingzy.ledger.ui.feature.settings.SettingsScreen
import cn.turingzy.ledger.ui.feature.wallet.WalletScreen
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

@Composable
fun LedgerNavHost(
    currentDestination: TopLevelDestination,
    showRecordSheet: Boolean,
    onRecordSheetDismiss: () -> Unit,
    onNavigateToRecord: () -> Unit,
    modifier: Modifier = Modifier,
    onBillSelectionVisibleChange: (Boolean) -> Unit = {},
    onSubpageChange: (TopLevelDestination, Boolean) -> Unit = { _, _ -> },
    onTaskTopChange: (TopLevelDestination, String, Int?) -> Unit = { _, _, _ -> },
) {
    RetainedDestinationHost(currentDestination, modifier) { destination ->
        val navController = rememberNavController()
        if (destination == TopLevelDestination.BILL || destination == TopLevelDestination.ANALYTICS) {
            val entry by navController.currentBackStackEntryAsState()
            val isSubpage = entry?.destination?.isTemporaryTask() == true
            LaunchedEffect(isSubpage) { onSubpageChange(destination, isSubpage) }
        }
        NavHost(
            navController = navController,
            startDestination = destination.route,
            modifier = Modifier.clipToBounds(),
            enterTransition = { PageTransitions.enter },
            exitTransition = {
                if (targetState.destination.isTemporaryTask()) ExitTransition.KeepUntilTransitionsFinished
                else PageTransitions.exit
            },
            popEnterTransition = {
                if (initialState.destination.isTemporaryTask()) EnterTransition.None else PageTransitions.popEnter
            },
            popExitTransition = { PageTransitions.popExit },
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
                    composable<Route.BillSearch>(
                        enterTransition = { PageTransitions.taskEnter },
                        popExitTransition = { PageTransitions.taskPopExit },
                    ) { searchEntry ->
                        TemporaryTaskContent(searchEntry.id, { onTaskTopChange(destination, searchEntry.id, it) }) {
                            BillSearchScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
                TopLevelDestination.ANALYTICS -> {
                    composable<Route.Analytics> {
                        AnalyticsScreen(onOpenCalendar = { date ->
                            navController.navigate(Route.ConsumptionCalendar(YearMonth.from(date).toString())) {
                                launchSingleTop = true
                            }
                        })
                    }
                    composable<Route.ConsumptionCalendar>(
                        enterTransition = { PageTransitions.taskEnter },
                        popExitTransition = { PageTransitions.taskPopExit },
                    ) { calendarEntry ->
                        val route = calendarEntry.toRoute<Route.ConsumptionCalendar>()
                        val detailsViewModel: TransactionDetailsViewModel = hiltViewModel()
                        val snackbar = remember { SnackbarHostState() }
                        val scope = rememberCoroutineScope()
                        var recordDate by rememberSaveable { mutableStateOf<Long?>(null) }
                        TemporaryTaskContent(calendarEntry.id, { onTaskTopChange(destination, calendarEntry.id, it) }) {
                            ConsumptionCalendarScreen(
                                initialMonth = YearMonth.parse(route.month),
                                onBack = { navController.popBackStack() },
                                onRecordTransaction = { recordDate = it.toEpochDay() },
                                onTransactionClick = detailsViewModel::selectTransaction,
                                snackbarHostState = snackbar,
                            )
                        }
                        val recording = recordDate
                        if (recording == null) {
                            TransactionDetailsScreen(
                                onLoadFailure = { message -> scope.launch { snackbar.showSnackbar(message) } },
                                viewModel = detailsViewModel,
                            )
                        } else {
                            RecordCreateSheet(LocalDate.ofEpochDay(recording), onDismiss = { recordDate = null })
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

/** Search and the calendar cover their parent pages with a vertical transition. */
private fun NavDestination.isTemporaryTask(): Boolean =
    hasRoute<Route.BillSearch>() || hasRoute<Route.ConsumptionCalendar>()

/** Reports the moving top edge until the full-screen task leaves composition. */
@Composable
private fun TemporaryTaskContent(entryId: String, onTopChange: (Int?) -> Unit, content: @Composable () -> Unit) {
    val reportTop by rememberUpdatedState(onTopChange)
    DisposableEffect(entryId) {
        onDispose { reportTop(null) }
    }
    Box(Modifier.fillMaxSize().onLayoutRectChanged(debounceMillis = 0) {
        reportTop(it.boundsInRoot.top)
    }) { content() }
}
