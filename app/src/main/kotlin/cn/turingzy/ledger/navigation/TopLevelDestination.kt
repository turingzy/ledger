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
    data class ConsumptionCalendar(val month: String) : Route

    @Serializable
    data object Wallet : Route

    @Serializable
    data object Settings : Route
}

enum class TopLevelDestination(
    val icon: ImageVector,
    val labelText: String,
    val route: Route,
) {
    LEDGER(
        icon = MaterialSymbols.Receipt,
        labelText = "账本",
        route = Route.Ledger,
    ),
    ANALYTICS(
        icon = MaterialSymbols.BarChart,
        labelText = "统计",
        route = Route.Analytics,
    ),
    WALLET(
        icon = MaterialSymbols.Wallet,
        labelText = "钱包",
        route = Route.Wallet,
    ),
    SETTINGS(
        icon = MaterialSymbols.Settings,
        labelText = "设置",
        route = Route.Settings,
    ),
}
