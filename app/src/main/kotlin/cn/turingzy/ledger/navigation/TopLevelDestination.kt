package cn.turingzy.ledger.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Settings
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
        selectedIcon = Icons.AutoMirrored.Filled.ReceiptLong,
        unselectedIcon = Icons.AutoMirrored.Outlined.ReceiptLong,
        labelText = "账本",
        route = Route.Ledger,
    ),
    ANALYTICS(
        selectedIcon = Icons.Filled.AutoGraph,
        unselectedIcon = Icons.Outlined.AutoGraph,
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
