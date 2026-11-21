package cn.turingzy.ledger.navigation

import androidx.annotation.StringRes
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable
    data object Bill : Route

    @Serializable
    data object BillSearch : Route

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
    val selectedIcon: ImageVector,
    @StringRes val labelResource: Int,
    val route: Route,
) {
    BILL(
        icon = MaterialSymbols.Home,
        selectedIcon = MaterialSymbols.HomeFilled,
        labelResource = R.string.ui_bill,
        route = Route.Bill,
    ),
    ANALYTICS(
        icon = MaterialSymbols.Leaderboard,
        selectedIcon = MaterialSymbols.LeaderboardFilled,
        labelResource = R.string.ui_analytics,
        route = Route.Analytics,
    ),
    WALLET(
        icon = MaterialSymbols.CreditCard,
        selectedIcon = MaterialSymbols.CreditCardFilled,
        labelResource = R.string.ui_wallet,
        route = Route.Wallet,
    ),
    SETTINGS(
        icon = MaterialSymbols.Settings,
        selectedIcon = MaterialSymbols.SettingsFilled,
        labelResource = R.string.ui_settings,
        route = Route.Settings,
    ),
}
