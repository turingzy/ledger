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
