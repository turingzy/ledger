package cn.turingzy.ledger.ui.component.picker

import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.component.base.DialogWindowBase
import cn.turingzy.ledger.ui.component.base.DialogActions
import cn.turingzy.ledger.ui.component.base.dialogContentPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import java.time.LocalDate

/**
 * 记账应用通用日期选择弹窗。
 *
 * 包装 [LedgerDatePicker] 于通用弹窗外框 [DialogWindowBase] 中，提供单日选择以及取消/确定操作按钮。
 *
 * @param initialDate 弹窗打开时的初始选中日期。
 * @param onDismiss 点击遮罩层或取消按钮时的关闭回调。
 * @param onConfirm 点击确认按钮时的回调，参数为最终选中的 [LocalDate]。
 * @param modifier 应用于弹窗主体表面 [SurfaceBase] 的修饰符。
 */
@Composable
internal fun LedgerDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,

) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var day by rememberSaveable(initialDate) { mutableLongStateOf(initialDate.toEpochDay()) }
    val date = LocalDate.ofEpochDay(day)
    DialogWindowBase(onDismiss) {
        SurfaceBase(
            modifier.fillMaxWidth().testTag("ledgerDatePickerDialog"),
            shape = LedgerStyle.shapes.dialog,
            color = LedgerStyle.colors.overlay, contentColor = LedgerStyle.colors.textPrimary,
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = LedgerStyle.dimensions.dialogMaxHeight).dialogContentPadding(LedgerStyle.spacing.extraLarge),
                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
            ) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    LedgerDatePicker(date) { day = it.toEpochDay() }
                }
                DialogActions(onCancel = onDismiss, onConfirm = { onConfirm(date) })
            }
        }
    }
}
