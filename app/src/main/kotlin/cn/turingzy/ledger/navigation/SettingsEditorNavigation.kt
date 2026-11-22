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

internal const val SettingsEditorList = "list"
internal const val SettingsEditorCreate = "create"

/** Keeps the management page stationary while a creation task covers it. */
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

/** Registers the vertical transition only for the creation route. */
internal fun NavGraphBuilder.settingsCreatePage(content: @Composable (NavBackStackEntry) -> Unit) {
    composable(
        route = SettingsEditorCreate,
        enterTransition = { PageTransitions.taskEnter },
        popExitTransition = { PageTransitions.taskPopExit },
    ) { entry -> content(entry) }
}

/** Prevents repeated Add taps from stacking multiple copies of the same form. */
internal fun NavController.openSettingsCreatePage() {
    navigate(SettingsEditorCreate) { launchSingleTop = true }
}
