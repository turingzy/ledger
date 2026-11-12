package cn.turingzy.ledger.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

internal enum class SettingsDestination { ROOT, CARDS, PARSE_RULES, BILL_TRANSFER, BACKUP, ABOUT, OPEN_SOURCE_LICENSES }

@Composable
internal fun SettingsNavigation(
    onDetailVisibleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (SettingsDestination, (SettingsDestination) -> Unit, () -> Unit) -> Unit,
) {
    val controller = rememberNavController()
    val entry by controller.currentBackStackEntryAsState()
    val visibleEntries by controller.visibleEntries.collectAsStateWithLifecycle()
    // Keep the outgoing page height stable until its exit animation finishes.
    val detailVisible = (visibleEntries + listOfNotNull(entry)).any {
        it.destination.route?.let { route -> route != SettingsDestination.ROOT.name } == true
    }
    LaunchedEffect(detailVisible) { onDetailVisibleChange(detailVisible) }
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
