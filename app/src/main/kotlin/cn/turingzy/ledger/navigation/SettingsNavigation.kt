package cn.turingzy.ledger.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

internal enum class SettingsDestination { ROOT, GENERAL, CARDS, PARSE_RULES, BILL_TRANSFER, ABOUT, OPEN_SOURCE_LICENSES }

/** Coordinates settings subpages within the settings tab. */
@Composable
internal fun SettingsNavigation(
    modifier: Modifier = Modifier,
    onSubpageChange: (Boolean) -> Unit = {},
    content: @Composable (SettingsDestination, (SettingsDestination) -> Unit, () -> Unit) -> Unit,
) {
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    val isSubpage = entry?.destination?.route?.let { it != SettingsDestination.ROOT.name } == true
    LaunchedEffect(isSubpage) { onSubpageChange(isSubpage) }
    NavHost(
        navController = controller,
        startDestination = SettingsDestination.ROOT.name,
        modifier = modifier,
        enterTransition = { slideInHorizontally(tween(240)) { it } },
        exitTransition = { slideOutHorizontally(tween(240)) { -it / 4 } },
        popEnterTransition = { slideInHorizontally(tween(240)) { -it / 4 } },
        popExitTransition = { slideOutHorizontally(tween(240)) { it } },
    ) {
        SettingsDestination.entries.forEach { destination ->
            composable(destination.name) {
                content(destination, { controller.navigate(it.name) { launchSingleTop = true } }, { controller.popBackStack() })
            }
        }
    }
}
