package cn.turingzy.ledger.ui.component

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import cn.turingzy.ledger.navigation.TopLevelDestination

@Composable
fun LedgerBottomBar(
    destinations: List<TopLevelDestination>,
    currentDestination: TopLevelDestination,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
    ) {
        destinations.forEach { destination ->
            val selected = destination == currentDestination
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigateToDestination(destination) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.labelText,
                    )
                },
                label = {
                    Text(text = destination.labelText)
                },
            )
        }
    }
}
