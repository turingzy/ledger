package cn.turingzy.ledger.ui.feature.bill.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.transaction.TransactionDetail
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.feature.bill.DayTransactionRowItem
import cn.turingzy.ledger.ui.interaction.rememberUiAction
import cn.turingzy.ledger.ui.interaction.rememberUiIndication
import cn.turingzy.ledger.ui.theme.LedgerStyle
import kotlin.math.roundToInt

private val SwipeActionWidth = 48.dp
private val SwipeActionsWidth = SwipeActionWidth * 3

@Composable
internal fun BillSwipeRow(
    detail: TransactionDetail,
    selecting: Boolean,
    selected: Boolean,
    enabled: Boolean,
    revealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LedgerStyle.colors
    val width = with(LocalDensity.current) { SwipeActionsWidth.toPx() }
    val canSwipe = enabled && !selecting
    var draggingOffset by remember(canSwipe) { mutableStateOf<Float?>(null) }
    var dragging by remember(canSwipe) { mutableStateOf(false) }
    val slideOffset = remember { Animatable(0f) }
    LaunchedEffect(revealed, canSwipe, width, dragging) {
        if (!dragging) {
            draggingOffset?.let { slideOffset.snapTo(it) }
            draggingOffset = null
            slideOffset.animateTo(if (revealed && canSwipe) width else 0f, tween(200))
        }
    }
    val offset = if (selecting) 0f else draggingOffset ?: slideOffset.value
    val updateRevealed by rememberUpdatedState(onRevealChange)
    val wasRevealed by rememberUpdatedState(revealed)

    Box(Modifier.fillMaxWidth().clipToBounds()) {
        if (offset > 0f) Box(Modifier.matchParentSize().background(colors.background)) {
            Row(
                Modifier.align(Alignment.CenterEnd).width(SwipeActionsWidth).fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BillSwipeAction(MaterialSymbols.FileCopy, R.string.bill_copy_action, R.string.bill_copy_transaction, colors.info, enabled, onCopy)
                BillSwipeAction(MaterialSymbols.EditSquare, R.string.bill_edit_action, R.string.bill_edit_transaction, colors.textPrimary, enabled, onEdit)
                BillSwipeAction(MaterialSymbols.Delete, R.string.bill_delete_action, R.string.ui_delete_transaction, colors.expense, enabled, onDelete)
            }
        }
        val dragModifier = if (canSwipe) Modifier.pointerInput(width) {
            var initialOpen = false
            detectHorizontalDragGestures(
                onDragStart = {
                    initialOpen = wasRevealed
                    draggingOffset = slideOffset.value
                    dragging = true
                    updateRevealed(true)
                },
                onHorizontalDrag = { change, delta ->
                    change.consume()
                    draggingOffset = ((draggingOffset ?: 0f) - delta).coerceIn(0f, width)
                },
                onDragEnd = {
                    updateRevealed((draggingOffset ?: 0f) >= width / 2f)
                    dragging = false
                },
                onDragCancel = {
                    updateRevealed(initialOpen)
                    dragging = false
                },
            )
        } else Modifier
        val corner = 20.dp * (offset / width).coerceIn(0f, 1f)
        Row(
            Modifier.fillMaxWidth().offset { IntOffset(-offset.roundToInt(), 0) }
                .clip(RoundedCornerShape(corner)).background(colors.surface)
                .then(dragModifier)
                .then(if (selecting) Modifier.semantics { toggleableState = ToggleableState(selected) } else Modifier)
                .combinedClickable(
                    enabled = enabled,
                    role = if (selecting) Role.Checkbox else Role.Button,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberUiIndication(),
                    onClick = rememberUiAction(onClick),
                    onLongClick = if (selecting) null else onLongClick,
                    onLongClickLabel = stringResource(R.string.bill_select_transaction),
                    hapticFeedbackEnabled = false,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AnimatedVisibility(
                visible = selecting,
                enter = expandHorizontally(tween(200), expandFrom = Alignment.Start) + fadeIn(tween(200)),
                exit = shrinkHorizontally(tween(200), shrinkTowards = Alignment.Start) + fadeOut(tween(160)),
            ) {
                BillSelectionIndicator(selected, Modifier.padding(start = LedgerStyle.dimensions.transactionRowHorizontal))
            }
            DayTransactionRowItem(detail, Modifier.weight(1f), onClick = null)
        }
    }
}

@Composable
private fun BillSwipeAction(icon: ImageVector, label: Int, description: Int, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    val actionDescription = stringResource(description)
    Column(
        Modifier.width(SwipeActionWidth).fillMaxHeight().clip(LedgerStyle.shapes.control)
            .semantics { contentDescription = actionDescription }
            .clickable(
                enabled = enabled, role = Role.Button, interactionSource = null, indication = null,
                onClick = rememberUiAction(onClick),
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(30.dp).background(tint.copy(alpha = 0.10f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp), tint = tint)
        }
        Text(stringResource(label), style = LedgerStyle.typography.caption, color = LedgerStyle.colors.textSecondary, maxLines = 1)
    }
}

@Composable
internal fun BillSelectionIndicator(checked: Boolean, modifier: Modifier = Modifier) {
    Icon(
        if (checked) MaterialSymbols.RadioButtonChecked else MaterialSymbols.RadioButtonUnchecked,
        contentDescription = null,
        modifier = modifier.size(LedgerStyle.icons.inline),
        tint = if (checked) LedgerStyle.colors.info else LedgerStyle.colors.textSecondary,
    )
}
