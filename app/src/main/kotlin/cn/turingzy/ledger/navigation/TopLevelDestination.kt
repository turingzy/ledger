package cn.turingzy.ledger.navigation

import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Ledger : Route

    @Serializable
    data object Analytics : Route

    @Serializable
    data object Wallet : Route

    @Serializable
    data object Settings : Route
}

enum class TopLevelDestination(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelText: String,
    val route: Route,
) {
    LEDGER(
        selectedIcon = MaterialSymbols.BookFilled,
        unselectedIcon = MaterialSymbols.Book,
        labelText = "账本",
        route = Route.Ledger,
    ),
    ANALYTICS(
        selectedIcon = MaterialSymbols.BarChartFilled,
        unselectedIcon = MaterialSymbols.BarChart,
        labelText = "统计",
        route = Route.Analytics,
    ),
    WALLET(
        selectedIcon = MaterialSymbols.AccountBalanceWalletFilled,
        unselectedIcon = MaterialSymbols.AccountBalanceWallet,
        labelText = "钱包",
        route = Route.Wallet,
    ),
    SETTINGS(
        selectedIcon = MaterialSymbols.SettingsFilled,
        unselectedIcon = MaterialSymbols.Settings,
        labelText = "设置",
        route = Route.Settings,
    ),
}
