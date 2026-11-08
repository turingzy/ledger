package cn.turingzy.ledger.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import cn.turingzy.ledger.ui.component.LedgerIcons
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
        selectedIcon = LedgerIcons.LedgerFilled,
        unselectedIcon = LedgerIcons.LedgerOutline,
        labelText = "账本",
        route = Route.Ledger,
    ),
    ANALYTICS(
        selectedIcon = LedgerIcons.AnalyticsFilled,
        unselectedIcon = LedgerIcons.AnalyticsOutline,
        labelText = "统计",
        route = Route.Analytics,
    ),
    WALLET(
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet,
        labelText = "钱包",
        route = Route.Wallet,
    ),
    SETTINGS(
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        labelText = "设置",
        route = Route.Settings,
    ),
}
