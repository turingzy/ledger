package cn.turingzy.ledger.ui.component.base

import android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import cn.turingzy.ledger.ui.theme.LedgerStyle

/**
 * 弹窗窗口基础容器。
 *
 * 封装 Material [Dialog]，禁用系统默认边距以便接管安全区域与软键盘避让，并通过 [Modifier.dialogBounds] 控制最大宽度与水平外边距。
 *
 * @param onDismissRequest 请求关闭弹窗时的回调。
 * @param modifier 应用于弹窗内部内容容器的修饰符。
 * @param content 弹窗内容插槽。
 */
@Composable
fun DialogWindowBase(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            if (window != null) {
                if ((window.attributes.flags and FLAG_DIM_BEHIND) == 0) window.addFlags(FLAG_DIM_BEHIND)
                if (window.attributes.dimAmount != LedgerStyle.opacity.modalScrim) {
                    window.setDimAmount(LedgerStyle.opacity.modalScrim)
                }
            }
        }
        Box(
            modifier.dialogBounds(),
        ) {
            content()
        }
    }
}

/** 新建交易和新增账户表单共用的展开高度，包含底部安全区。 */
@Composable
internal fun primaryActionSheetHeight(): Dp = LocalConfiguration.current.screenHeightDp.dp * 0.8f

/**
 * 基础模态底部抽屉组件。
 *
 * 应用统一的圆角形状、半透明遮罩背景与明暗状态栏配置，手势和动效由 Material3 底层驱动。
 *
 * @param onDismissRequest 请求关闭底部抽屉时的回调。
 * @param sheetState 底部抽屉的状态 [SheetState]。
 * @param modifier 应用于抽屉的修饰符。
 * @param containerColor 抽屉背景颜色。
 * @param contentColor 抽屉内部内容主色。
 * @param dragHandle 顶部拖动手柄插槽，为 null 时不显示。
 * @param contentHandlesInsets 内容是否自行处理窗口安全区边距（为 true 时禁用安全区内边距）。
 * @param content 抽屉垂直内容插槽。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetBase(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    containerColor: Color = LedgerStyle.colors.overlay,
    contentColor: Color = LedgerStyle.colors.textPrimary,
    dragHandle: (@Composable () -> Unit)? = null,
    contentHandlesInsets: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    // The status bar is drawn over the host page, and its icons stay in place as the sheet closes.
    val lightBars = MaterialTheme.colorScheme.background.luminance() > 0.5f
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = LedgerStyle.spacing.none,
        shape = LedgerStyle.shapes.bottomSheet,
        scrimColor = Color.Black.copy(alpha = LedgerStyle.opacity.modalScrim),
        dragHandle = dragHandle,
        properties = ModalBottomSheetProperties(
            isAppearanceLightStatusBars = lightBars,
            isAppearanceLightNavigationBars = lightBars,
        ),
        contentWindowInsets = { if (contentHandlesInsets) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing },
        content = {
            CompositionLocalProvider(LocalSurfaceBackground provides containerColor) { content() }
        },
    )
}

/**
 * 底部抽屉顶部标题栏组件。
 *
 * 标题在水平方向独立居中，左侧放置关闭按钮，右侧放置可选的操作项。
 *
 * @param closeDescription 关闭按钮的无障碍描述。
 * @param onClose 点击关闭按钮时的回调。
 * @param modifier 应用于标题栏的修饰符。
 * @param action 可选的右侧操作插槽。
 * @param title 居中标题内容插槽。
 */
@Composable
internal fun BottomSheetHeaderBase(
    closeDescription: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    Box(
        modifier.fillMaxWidth().heightIn(min = LedgerStyle.dimensions.touchTarget),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.padding(horizontal = LedgerStyle.dimensions.touchTarget), contentAlignment = Alignment.Center) {
            title()
        }
        IconButtonBase(
            MaterialSymbols.Close, closeDescription, onClose,
            modifier = Modifier.align(Alignment.CenterStart),
            showPressFeedback = false,
        )
        if (action != null) Box(Modifier.align(Alignment.CenterEnd)) { action() }
    }
}

/**
 * 基础警报/确认对话框。
 *
 * 包含可选标题、支持滚动的正文内容以及底部操作按钮（取消、确认、次要操作）。
 *
 * @param onDismissRequest 请求关闭对话框时的回调。
 * @param onConfirm 点击确认按钮时的回调。
 * @param confirmLabel 确认按钮的显示文本。
 * @param confirmEnabled 确认按钮是否可用。
 * @param destructive 确认操作是否具有破坏性（为 true 时确认按钮呈警告红色）。
 * @param showCancel 是否显示取消按钮。
 * @param secondaryAction 可选的次要操作按钮插槽。
 * @param title 对话框标题内容插槽。
 * @param text 对话框正文说明内容插槽。
 */
@Composable
internal fun BaseAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmLabel: String = stringResource(R.string.ui_confirm),
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    showCancel: Boolean = true,
    secondaryAction: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    DialogWindowBase(onDismissRequest) {
        SurfaceBase(
            Modifier.fillMaxWidth(), shape = LedgerStyle.shapes.confirmationDialog,
            color = LedgerStyle.colors.overlay, contentColor = LedgerStyle.colors.textPrimary,
        ) {
            Column(Modifier.dialogContentPadding(LedgerStyle.spacing.block), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
                    if (title != null) ProvideTextStyle(MaterialTheme.typography.titleLarge, title)
                    if (text != null) ProvideTextStyle(MaterialTheme.typography.bodyMedium, text)
                }
                DialogActions(
                    onCancel = onDismissRequest.takeIf { showCancel },
                    onConfirm = onConfirm,
                    confirmLabel = confirmLabel,
                    confirmEnabled = confirmEnabled,
                    destructive = destructive,
                    secondaryAction = secondaryAction,
                )
            }
        }
    }
}

/**
 * 设置对话框内容的内边距，保持四周留白并微调底部边距以更贴近操作按钮。
 *
 * @param inset 顶部与水平方向的内边距大小。
 */
internal fun Modifier.dialogContentPadding(inset: Dp): Modifier =
    padding(start = inset, top = inset, end = inset, bottom = LedgerStyle.spacing.medium)

/**
 * 对话框底部操作按钮排版组件。
 *
 * 使用 [FlowRow] 布局按钮，自动折行以适配窄屏或过长的多语言文案。
 *
 * @param onCancel 点击取消时的回调，为 null 时不显示取消按钮。
 * @param onConfirm 点击确认时的回调。
 * @param confirmLabel 确认按钮的显示文本。
 * @param cancelEnabled 取消按钮是否可用。
 * @param confirmEnabled 确认按钮是否可用。
 * @param destructive 确认操作是否具有破坏性。
 * @param secondaryAction 可选的次要操作按钮插槽。
 */
@Composable
internal fun DialogActions(
    onCancel: (() -> Unit)?,
    onConfirm: () -> Unit,
    confirmLabel: String = stringResource(R.string.ui_confirm),
    cancelEnabled: Boolean = true,
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    secondaryAction: (@Composable () -> Unit)? = null,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (onCancel == null) Arrangement.End else Arrangement.SpaceBetween,
    ) {
        if (onCancel != null) {
            ButtonBase(
                stringResource(R.string.ui_cancel), onCancel, enabled = cancelEnabled,
                variant = ButtonVariantBase.Text, tone = ButtonToneBase.Neutral,
            )
        }
        secondaryAction?.invoke()
        ButtonBase(
            confirmLabel, onConfirm, enabled = confirmEnabled, variant = ButtonVariantBase.Text,
            tone = if (destructive) ButtonToneBase.Destructive else ButtonToneBase.Default,
        )
    }
}

/**
 * 在输入法（软键盘）弹出期间保持底部抽屉锚点稳定的布局修饰符。
 */
internal fun Modifier.keepSheetAnchorDuringIme(): Modifier = composed {
    val imeInsets = WindowInsets.ime
    Modifier.layout { measurable, constraints ->
        val keyboardHeight = imeInsets.getBottom(this)
        val placeable = measurable.measure(constraints.copy(maxHeight = constraints.maxHeight + keyboardHeight))
        layout(placeable.width, constraints.constrainHeight(placeable.height)) {
            placeable.place(0, 0)
        }
    }
}

/**
 * 约束对话框最大宽度并适配安全区域与软键盘避让的修饰符。
 */
@Composable
private fun Modifier.dialogBounds(): Modifier =
    widthIn(max = LedgerStyle.dimensions.wideDialogMaxWidth).fillMaxWidth()
        .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime))
        .padding(horizontal = LedgerStyle.spacing.section)
