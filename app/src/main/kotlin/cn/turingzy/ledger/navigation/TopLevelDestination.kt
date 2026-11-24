package cn.turingzy.ledger.navigation

import androidx.annotation.StringRes
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

/**
 * 应用类型安全的 Compose Navigation 路由定义密封接口。
 */
@Serializable
sealed interface Route {
    /** 账单主页面路由。 */
    @Serializable
    data object Bill : Route

    /** 账单全局搜索全屏任务页面路由。 */
    @Serializable
    data object BillSearch : Route

    /** 统计分析主页面路由。 */
    @Serializable
    data object Analytics : Route

    /** 钱包主页面路由。 */
    @Serializable
    data object Wallet : Route

    /** 设置主页面路由。 */
    @Serializable
    data object Settings : Route
}

/**
 * 底部主导航栏的顶级目的地枚举。
 *
 * 定义主导航栏四个核心模块（账单、统计、钱包、设置）所关联的图标、文案与起始路由。
 *
 * @property icon 未选中的常规状态图标。
 * @property selectedIcon 激活选中状态的高亮图标。
 * @property labelResource 标签名称字符串资源 ID。
 * @property route 对应的顶级起始页面路由。
 */
enum class TopLevelDestination(
    val icon: ImageVector,
    val selectedIcon: ImageVector,
    @StringRes val labelResource: Int,
    val route: Route,
) {
    /** 账单流水页面。 */
    BILL(
        icon = MaterialSymbols.Home,
        selectedIcon = MaterialSymbols.HomeFilled,
        labelResource = R.string.ui_bill,
        route = Route.Bill,
    ),
    /** 统计报表页面。 */
    ANALYTICS(
        icon = MaterialSymbols.Leaderboard,
        selectedIcon = MaterialSymbols.LeaderboardFilled,
        labelResource = R.string.ui_analytics,
        route = Route.Analytics,
    ),
    /** 钱包账户页面。 */
    WALLET(
        icon = MaterialSymbols.CreditCard,
        selectedIcon = MaterialSymbols.CreditCardFilled,
        labelResource = R.string.ui_wallet,
        route = Route.Wallet,
    ),
    /** 应用设置页面。 */
    SETTINGS(
        icon = MaterialSymbols.Settings,
        selectedIcon = MaterialSymbols.SettingsFilled,
        labelResource = R.string.ui_settings,
        route = Route.Settings,
    ),
}
