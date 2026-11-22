package cn.turingzy.ledger.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 设置模块内部导航子目的地枚举。
 */
internal enum class SettingsDestination {
    /** 设置主入口根页面。 */
    ROOT,
    /** 常规偏好设置（主货币、主题模式、语言等）。 */
    GENERAL,
    /** 账户与账本卡片外观个性化设置。 */
    CARDS,
    /** 账单导入自定义解析规则管理。 */
    PARSE_RULES,
    /** 数据传输（备份、恢复与外部账单导入导出）。 */
    BILL_TRANSFER,
    /** 关于应用。 */
    ABOUT,
    /** 开源许可证说明。 */
    OPEN_SOURCE_LICENSES,
}

/**
 * 设置标签页内部的二级导航协调宿主。
 *
 * 在“设置”顶级目的地内部承载各子页面的切换，监听当前回退栈状态，
 * 当进入非 ROOT 的二级子页面时及时上报，以便主页面联动隐藏全局底部导航栏。
 *
 * @param modifier 宿主容器修饰符。
 * @param onSubpageChange 二级子页面显隐状态变化回调（true 表示处于二级页面，需隐藏底部栏）。
 * @param content 子页面渲染内容 Lambda，提供目标目的地枚举、导航跳转回调以及返回上一页回调。
 */
@Composable
internal fun SettingsNavigation(
    modifier: Modifier = Modifier,
    onSubpageChange: (Boolean) -> Unit = {},
    content: @Composable (SettingsDestination, (SettingsDestination) -> Unit, () -> Unit) -> Unit,
) {
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    // The bottom bar follows the destination as soon as navigation commits.
    val isSubpage = entry?.destination?.route?.let { it != SettingsDestination.ROOT.name } == true
    LaunchedEffect(isSubpage) { onSubpageChange(isSubpage) }
    NavHost(
        navController = controller,
        startDestination = SettingsDestination.ROOT.name,
        modifier = modifier.clipToBounds(),
        enterTransition = { PageTransitions.enter },
        exitTransition = { PageTransitions.exit },
        popEnterTransition = { PageTransitions.popEnter },
        popExitTransition = { PageTransitions.popExit },
    ) {
        SettingsDestination.entries.forEach { destination ->
            composable(destination.name) {
                Box(Modifier.fillMaxSize().background(LedgerStyle.colors.background)) {
                    content(destination, { controller.navigate(it.name) { launchSingleTop = true } }, { controller.popBackStack() })
                }
            }
        }
    }
}
