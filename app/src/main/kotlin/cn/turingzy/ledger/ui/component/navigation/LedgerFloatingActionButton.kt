package cn.turingzy.ledger.ui.component.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.base.ActionButtonBase
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 顶级页面共用的悬浮主操作按钮，统一胶囊尺寸和尾部图标布局。
 */
@Composable
fun LedgerFloatingActionButton(
    label: String,
    icon: ImageVector,
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f),
        modifier = modifier,
    ) {
        // Reserve touch space around the capsule so visual effects follow its measured outline.
        Box(Modifier.minimumInteractiveComponentSize(), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                ActionButtonBase(
                    onClick = onClick,
                    modifier = Modifier.heightIn(min = 36.dp)
                        .shadow(LedgerStyle.dimensions.floatingActionShadow, LedgerStyle.shapes.pill, clip = false),
                    shape = LedgerStyle.shapes.pill,
                    elevation = null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor,
                    ),
                    contentPadding = PaddingValues(
                        horizontal = LedgerStyle.spacing.extraLarge,
                        vertical = LedgerStyle.spacing.extraSmall,
                    ),
                    interaction = UiInteractionKind.Navigation,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.inlineGap),
                    ) {
                        Text(label, style = LedgerStyle.typography.floatingActionLabel)
                        Icon(icon, contentDescription = null, modifier = Modifier.size(LedgerStyle.icons.default))
                    }
                }
            }
        }
    }
}
