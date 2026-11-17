package cn.turingzy.ledger.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import cn.turingzy.ledger.ui.theme.LedgerStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun SummaryCardIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.scrim.copy(alpha = LedgerStyle.opacity.cardIconBackground),
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    showPressFeedback: Boolean = true,
    iconSize: Dp = LedgerStyle.icons.default,
) {
    Box(
        modifier.size(LedgerStyle.dimensions.compactIconButton).clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = if (showPressFeedback) LocalIndication.current else null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = contentColor.copy(alpha = contentColor.alpha * if (enabled) 1f else LedgerStyle.opacity.disabled),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
internal fun TopBarIconButton(
    imageVector: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPressFeedback: Boolean = true,
    iconSize: Dp = LedgerStyle.icons.default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier.size(LedgerStyle.dimensions.compactIconButton).clip(CircleShape)
            .topBarPressFeedback(interactionSource, enabled = showPressFeedback)
            .clickable(
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector, contentDescription, Modifier.size(iconSize), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun TopBarBackButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    TopBarIconButton(
        imageVector = MaterialSymbols.ChevronLeft,
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier.size(LedgerStyle.dimensions.backButton),
        iconSize = LedgerStyle.icons.back,
    )
}

@Composable
private fun Modifier.topBarPressFeedback(
    interactionSource: MutableInteractionSource,
    enabled: Boolean,
    color: Color = MaterialTheme.colorScheme.onSurface,
): Modifier {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(interactionSource, enabled, color) {
        alpha.snapTo(0f)
        if (!enabled) return@LaunchedEffect
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> alpha.animateTo(LedgerStyle.opacity.toolbarPressed, tween(70))
                is PressInteraction.Release -> {
                    // A quick tap must remain visible even when down and up arrive in one frame.
                    alpha.animateTo(LedgerStyle.opacity.toolbarPressed, tween(40))
                    delay(40)
                    alpha.animateTo(0f, tween(140))
                }
                is PressInteraction.Cancel -> alpha.animateTo(0f, tween(100))
            }
        }
    }
    return drawBehind { drawRect(color.copy(alpha = alpha.value)) }
        .pointerInput(interactionSource, enabled) {
            if (!enabled) return@pointerInput
            awaitEachGesture {
                // Observe the initial pass without consuming events needed by clicks or scrolling.
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val press = PressInteraction.Press(down.position)
                interactionSource.tryEmit(press)
                var finished = false
                try {
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    interactionSource.tryEmit(
                        if (up == null) PressInteraction.Cancel(press) else PressInteraction.Release(press),
                    )
                    finished = true
                } finally {
                    if (!finished) interactionSource.tryEmit(PressInteraction.Cancel(press))
                }
            }
        }
}
