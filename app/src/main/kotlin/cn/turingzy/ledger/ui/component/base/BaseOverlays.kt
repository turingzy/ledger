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
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cn.turingzy.ledger.ui.theme.LedgerStyle

@Composable
fun BaseDialogWindow(
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
fun BaseModalBottomSheet(
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
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = containerColor,
        contentColor = contentColor,
        shape = shape,
        dragHandle = dragHandle,
        contentWindowInsets = { if (contentHandlesInsets) WindowInsets(0, 0, 0, 0) else WindowInsets.safeDrawing },
        content = content,
    )
}

@Composable
fun BaseAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    containerColor: Color = LedgerStyle.colors.surface,
    textContentColor: Color = LedgerStyle.colors.textPrimary,
    tonalElevation: Dp = LedgerStyle.spacing.none,
) {
    AlertDialog(
        onDismissRequest, confirmButton, modifier.dialogBounds(),
        dismissButton = dismissButton, title = title, text = text,
        shape = LedgerStyle.shapes.confirmationDialog,
        containerColor = containerColor, textContentColor = textContentColor,
        tonalElevation = tonalElevation,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    )
}

@Composable
private fun Modifier.dialogBounds(): Modifier =
    widthIn(max = LedgerStyle.dimensions.wideDialogMaxWidth).fillMaxWidth()
        .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime))
        .padding(horizontal = LedgerStyle.spacing.pageHorizontal)
