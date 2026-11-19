package cn.turingzy.ledger.ui.component.base

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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.turingzy.ledger.ui.theme.LedgerStyle

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
        Box(
            modifier.dialogBounds(),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheetBase(
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    containerColor: Color = LedgerStyle.colors.surface,
    contentColor: Color = LedgerStyle.colors.textPrimary,
    shape: Shape = LedgerStyle.shapes.bottomSheet,
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
        shape = shape,
        dragHandle = dragHandle,
        properties = ModalBottomSheetProperties(
            isAppearanceLightStatusBars = lightBars,
            isAppearanceLightNavigationBars = lightBars,
        ),
        contentWindowInsets = { if (contentHandlesInsets) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing },
        content = content,
    )
}

@Composable
internal fun BaseAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    confirmDescription: String = stringResource(R.string.ui_confirm),
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
    showCancel: Boolean = true,
    secondaryAction: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) {
    DialogWindowBase(onDismissRequest) {
        SurfaceBase(Modifier.fillMaxWidth(), shape = LedgerStyle.shapes.confirmationDialog, color = LedgerStyle.colors.surface) {
            Column(Modifier.padding(LedgerStyle.spacing.block), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
                    if (title != null) ProvideTextStyle(MaterialTheme.typography.titleLarge, title)
                    if (text != null) ProvideTextStyle(MaterialTheme.typography.bodyMedium, text)
                }
                Row(Modifier.fillMaxWidth()) {
                    if (showCancel) IconButtonBase(MaterialSymbols.Close, stringResource(R.string.ui_cancel), onDismissRequest)
                    Spacer(Modifier.weight(1f))
                    secondaryAction?.invoke()
                    IconButtonBase(
                        if (destructive) MaterialSymbols.Delete else MaterialSymbols.Check,
                        confirmDescription, onConfirm, enabled = confirmEnabled,
                        contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

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

@Composable
private fun Modifier.dialogBounds(): Modifier =
    widthIn(max = LedgerStyle.dimensions.wideDialogMaxWidth).fillMaxWidth()
        .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime))
        .padding(horizontal = LedgerStyle.spacing.pageHorizontal)
