package cn.turingzy.ledger.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.turingzy.ledger.ui.feature.analytics.AnalyticsScreen
import cn.turingzy.ledger.ui.feature.bill.BillScreen
import cn.turingzy.ledger.ui.feature.bill.search.BillSearchScreen
import cn.turingzy.ledger.ui.feature.settings.SettingsScreen
import cn.turingzy.ledger.ui.feature.wallet.WalletScreen

/**
 * 账本应用主导航宿主组件。
 *
 * 结合 [RetainedDestinationHost] 保持四大顶级目的地（账单、统计分析、钱包、设置）的状态持久化，
 * 并在各目的地内部建立 [NavHost]，以处理账单搜索等子页面的路由跳转。
 *
 * @param currentDestination 当前激活的顶级目的地。
 * @param showRecordSheet 是否显示快速记账半屏弹窗。
 * @param onRecordSheetDismiss 快速记账半屏弹窗关闭回调。
 * @param onNavigateToRecord 触发新建记账操作的回调。
 * @param modifier 宿主容器的 Modifier。
 * @param onBillSelectionVisibleChange 账单列表多选状态变更通知（用于调整主脚手架底部栏）。
 * @param onSubpageChange 某顶级目的地内部二级子页面状态变更通知。
 * @param onTaskTopChange 临时任务页面在屏幕上的顶部边缘位置更新回调。
 */
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
        if (destination == TopLevelDestination.BILL) {
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
                TopLevelDestination.ANALYTICS -> composable<Route.Analytics> { AnalyticsScreen() }
                TopLevelDestination.WALLET -> composable<Route.Wallet> { WalletScreen() }
                TopLevelDestination.SETTINGS -> composable<Route.Settings> {
                    SettingsScreen(onSubpageChange = { onSubpageChange(destination, it) })
                }
            }
        }
    }
}

/**
 * 判断当前目的地是否为覆盖在父级页面之上的全屏临时任务页。
 */
private fun NavDestination.isTemporaryTask(): Boolean = hasRoute<Route.BillSearch>()

/**
 * 包装全屏临时任务页内容，并在页面布局变化或离开组合时实时上报其顶部 Y 轴位置。
 *
 * @param entryId 当前导航回退栈条目的唯一标识。
 * @param onTopChange 顶部 Y 坐标变动回调（销毁时回调 null）。
 * @param content 待呈现的内容组件。
 */
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
