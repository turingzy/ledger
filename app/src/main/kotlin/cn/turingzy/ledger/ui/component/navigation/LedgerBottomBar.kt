package cn.turingzy.ledger.ui.component.navigation

import cn.turingzy.ledger.ui.component.base.BackdropStateBase
import cn.turingzy.ledger.ui.component.base.frostedBackdropBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import cn.turingzy.ledger.navigation.TopLevelDestination
import cn.turingzy.ledger.ui.interaction.rememberSystemClickFeedback

@Composable
internal fun LedgerBottomBar(
    destinations: List<TopLevelDestination>,
    currentDestination: TopLevelDestination,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: BackdropStateBase? = null,
    visible: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    val clickFeedback = rememberSystemClickFeedback()
    val visibility = animateFloatAsState(if (visible) 1f else 0f, tween(160), label = "BottomBarVisibility")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = BottomBarVerticalPadding)
            .graphicsLayer { alpha = visibility.value }
            .then(if (visible) Modifier else Modifier.clearAndSetSemantics { hideFromAccessibility() })
            .shadow(2.dp, CircleShape)
            .border(0.5.dp, colors.outlineVariant.copy(alpha = colors.outlineVariant.alpha * 0.5f), CircleShape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(BottomBarHeight)
                .frostedBackdropBase(backdrop, colors.surface.copy(alpha = 0.72f), 12.dp)
                .selectableGroup()
                .padding(horizontal = 4.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val selected = destination == currentDestination
                val interactionSource = remember(destination) { MutableInteractionSource() }
                val iconTint by animateColorAsState(
                    targetValue = if (selected) {
                        colors.primary.copy(alpha = 0.82f)
                    } else {
                        colors.onSurface
                    },
                    animationSpec = tween(durationMillis = 120),
                    label = "TabIconTint",
                )

                val selectionColor by animateColorAsState(
                    targetValue = if (selected) colors.primary.copy(alpha = 0.065f) else Color.Transparent,
                    animationSpec = tween(durationMillis = 120),
                    label = "TabSelectionColor",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(selectionColor)
                        .navigationPressFeedback(interactionSource)
                        .then(if (!visible) Modifier else Modifier.selectable(
                            selected = selected,
                            role = Role.Tab,
                            interactionSource = interactionSource,
                            indication = null,
                        ) {
                            if (!selected) {
                                clickFeedback()
                                onNavigateToDestination(destination)
                            }
                        }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
                ) {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(25.dp),
                    )
                    Text(
                        text = stringResource(destination.labelResource),
                        fontSize = LedgerStyle.typography.caption.fontSize,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = iconTint,
                        maxLines = 1,
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
            currentDestination = TopLevelDestination.BILL,
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
