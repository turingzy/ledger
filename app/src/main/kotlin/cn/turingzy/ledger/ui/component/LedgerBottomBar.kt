package cn.turingzy.ledger.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.navigation.TopLevelDestination

@Composable
fun LedgerBottomBar(
    destinations: List<TopLevelDestination>,
    currentDestination: TopLevelDestination,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = BottomBarVerticalPadding),
        shape = CircleShape,
        color = colors.surface,
        shadowElevation = 2.dp,
        border = BorderStroke(
            width = 0.5.dp,
            color = colors.outlineVariant.copy(alpha = colors.outlineVariant.alpha * 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight)
                .selectableGroup()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val selected = destination == currentDestination
                val interactionSource = remember(destination) { MutableInteractionSource() }
                val iconTint by animateColorAsState(
                    targetValue = if (selected) {
                        colors.onSurface
                    } else {
                        colors.onSurfaceVariant.copy(alpha = 0.60f)
                    },
                    animationSpec = tween(durationMillis = 120),
                    label = "TabIconTint",
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .navigationPressFeedback(interactionSource)
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            if (!selected) onNavigateToDestination(destination)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.labelText,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Ledger Selected", showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun LedgerBottomBarLedgerPreview() {
    cn.turingzy.ledger.ui.theme.LedgerTheme {
        LedgerBottomBar(
            destinations = TopLevelDestination.entries,
            currentDestination = TopLevelDestination.LEDGER,
            onNavigateToDestination = {},
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Analytics Selected", showBackground = true, backgroundColor = 0xFFF2F2F7)
@Composable
private fun LedgerBottomBarAnalyticsPreview() {
    cn.turingzy.ledger.ui.theme.LedgerTheme {
        LedgerBottomBar(
            destinations = TopLevelDestination.entries,
            currentDestination = TopLevelDestination.ANALYTICS,
            onNavigateToDestination = {},
        )
    }
}
