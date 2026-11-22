package cn.turingzy.ledger.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/** 设置管理项列表路由标识。 */
internal const val SettingsEditorList = "list"
/** 设置新建/编辑页面路由标识。 */
internal const val SettingsEditorCreate = "create"

/**
 * 设置项编辑器专用导航宿主组件。
 *
 * 用于诸如分类管理、账户卡面模板管理等包含“列表浏览”与“新建创建”的设置二级流程。
 * 在打开新建页面时使底层管理列表保持静止，新建页则以全屏任务垂直滑入覆盖。
 *
 * @param controller 导航控制器 [NavHostController]。
 * @param builder 导航图构建器作用域。
 */
@Composable
internal fun SettingsEditorNavHost(
    controller: NavHostController,
    builder: NavGraphBuilder.() -> Unit,
) {
    NavHost(
        navController = controller,
        startDestination = SettingsEditorList,
        modifier = Modifier.fillMaxSize().clipToBounds(),
        enterTransition = { PageTransitions.enter },
        exitTransition = {
            if (targetState.destination.route == SettingsEditorCreate) ExitTransition.KeepUntilTransitionsFinished
            else PageTransitions.exit
        },
        popEnterTransition = {
            if (initialState.destination.route == SettingsEditorCreate) EnterTransition.None
            else PageTransitions.popEnter
        },
        popExitTransition = { PageTransitions.popExit },
        builder = builder,
    )
}

/**
 * 在导航图中注册使用垂直滑入/滑出转场动效的“新建”任务页面。
 *
 * @param content 新建页面内容 Composable。
 */
internal fun NavGraphBuilder.settingsCreatePage(content: @Composable (NavBackStackEntry) -> Unit) {
    composable(
        route = SettingsEditorCreate,
        enterTransition = { PageTransitions.taskEnter },
        popExitTransition = { PageTransitions.taskPopExit },
    ) { entry -> content(entry) }
}

/**
 * 导航至设置项的新建页面。
 *
 * 开启 `launchSingleTop` 避免用户连续快速点击导致创建页面多次入栈。
 */
internal fun NavController.openSettingsCreatePage() {
    navigate(SettingsEditorCreate) { launchSingleTop = true }
}
