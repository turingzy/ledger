package cn.turingzy.ledger.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.navigation.TopLevelDestination

/**
 * 底部沉浸式贴边导航栏。
 *
 * 整体紧贴手机屏幕最底部，左右及底边与物理边框无缝衔接；
 * 上边缘采用平直直线收边与浅色细线分割，呼应手机底部自带圆角的物理特性；
 * 去除多余背景框，纯净呈现 Material Symbols (Weight 200, Grade 0, Optical Size 24px) 图标。
 */
@Composable
fun LedgerBottomBar(
    destinations: List<TopLevelDestination>,
    currentDestination: TopLevelDestination,
    onNavigateToDestination: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .height(56.dp)
                    .selectableGroup()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                destinations.forEach { destination ->
                    val selected = destination == currentDestination
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    // 点击交互中或当前激活状态均采用 Fill 形式
                    val isFilled = selected || isPressed

                    val iconScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.88f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                        label = "TabScale",
                    )

                    val iconTint by animateColorAsState(
                        targetValue = if (selected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.60f)
                        },
                        animationSpec = tween(durationMillis = 200),
                        label = "TabIconTint",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
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
                            imageVector = if (isFilled) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.labelText,
                            tint = iconTint,
                            modifier = Modifier
                                .size(28.dp)
                                .scale(iconScale),
                        )
                    }
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
