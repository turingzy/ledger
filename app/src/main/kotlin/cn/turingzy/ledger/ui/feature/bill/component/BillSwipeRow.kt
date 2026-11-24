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
import androidx.compose.foundation.shape.CornerSize
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

/**
 * 带有手势侧滑操作及多选状态的账单交易行组件。
 *
 * 在非多选模式下支持向左滑动显露“复制”、“编辑”、“删除”操作按钮，支持长按触发多选模式；
 * 在多选模式下显示左侧选择指示圈，点击切换选中状态。
 *
 * @param detail 交易详情数据 [TransactionDetail]。
 * @param selecting 是否处于多选编辑模式。
 * @param selected 当前项是否被选中。
 * @param enabled 是否允许用户交互。
 * @param revealed 侧滑操作区当前是否展开露出。
 * @param onRevealChange 侧滑展开状态变化回调。
 * @param onClick 单击事件回调（多选时切换选中，非多选且展开时收起，否则查看详情）。
 * @param onLongClick 长按事件回调（非多选模式下长按进入多选）。
 * @param onCopy 点击复制操作回调。
 * @param onEdit 点击编辑操作回调。
 * @param onDelete 点击删除操作回调。
 * @param showDate 时间显示是否包含日期（跨天搜索展示时为 true）。
 * @param timeText 调用方提供的时间文本。
 * @param roundTop 当前行是否需要裁剪顶部圆角（例如列表首项）。
 * @param roundBottom 当前行是否需要裁剪底部圆角（例如列表尾项）。
 */
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
    showDate: Boolean = false,
    roundTop: Boolean = false,
    roundBottom: Boolean = false,
    timeText: String? = null,
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

    // The foreground may cross the group's left edge while the action rail stays in place.
    Box(Modifier.fillMaxWidth()) {
        if (offset > 0f) Box(Modifier.matchParentSize().background(colors.background)) {
            Row(
                Modifier.align(Alignment.CenterEnd).width(SwipeActionsWidth).fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BillSwipeAction(MaterialSymbols.FileCopy, R.string.bill_copy_action, R.string.bill_copy_transaction, colors.info, enabled, onCopy)
                BillSwipeAction(MaterialSymbols.EditSquare, R.string.bill_edit_action, R.string.bill_edit_transaction, colors.textPrimary, enabled, onEdit)
                BillSwipeAction(MaterialSymbols.Delete, R.string.bill_delete_action, R.string.ui_delete_transaction, colors.destructive, enabled, onDelete)
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
        val movingCorner = CornerSize(20.dp * (offset / width).coerceIn(0f, 1f))
        val groupCorner = LedgerStyle.shapes.summaryCard.topStart
        val top = if (roundTop) groupCorner else movingCorner
        val bottom = if (roundBottom) groupCorner else movingCorner
        Row(
            Modifier.fillMaxWidth().offset { IntOffset(-offset.roundToInt(), 0) }
                .clip(RoundedCornerShape(top, top, bottom, bottom)).background(colors.surface)
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
                BillSelectionIndicator(selected, Modifier.padding(start = LedgerStyle.spacing.rowHorizontal), enabled = enabled)
            }
            DayTransactionRowItem(detail, Modifier.weight(1f), onClick = null, showDate = showDate, timeText = timeText)
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
            Icon(icon, contentDescription = null, modifier = Modifier.size(LedgerStyle.icons.inline), tint = tint)
        }
        Text(stringResource(label), style = LedgerStyle.typography.caption, color = LedgerStyle.colors.textSecondary, maxLines = 1)
    }
}

/**
 * 账单多选的复选标记，交互由所在行提供。
 *
 * @param checked 当前项是否选中。
 * @param modifier 外部修饰符。
 * @param enabled 当前操作是否可用。
 */
@Composable
internal fun BillSelectionIndicator(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = LedgerStyle.colors
    val tint = if (checked) colors.info else colors.textSecondary
    Icon(
        if (checked) MaterialSymbols.CheckBox else MaterialSymbols.CheckBoxOutlineBlank,
        contentDescription = null,
        modifier = modifier.size(LedgerStyle.icons.inline),
        tint = tint.copy(alpha = if (enabled) 1f else LedgerStyle.opacity.disabled),
    )
}
