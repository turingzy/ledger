package cn.turingzy.ledger.ui.component.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import cn.turingzy.ledger.ui.component.base.DialogWindowBase
import cn.turingzy.ledger.ui.component.base.DialogActions
import cn.turingzy.ledger.ui.component.base.dialogContentPadding
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 日期与时间联合选择弹窗组件。
 *
 * 包装 [LedgerDateTimePicker]，支持用户同时选择年月日以及具体时分，并支持通过 [isTimeAllowed] 进行时间合法性校验。
 *
 * @param initialTime 初始时间戳（毫秒）。
 * @param onDismiss 关闭或取消弹窗的回调。
 * @param onConfirm 确认选中时间的回调，参数为最终选中的毫秒时间戳。
 * @param isTimeAllowed 判断所选时间是否合法的校验函数（例如禁止选择未来时间），默认为始终允许。
 * @param invalidTimeMessage 当用户选择的时间不合法时展示的错误提示文本。
 */
@Composable
internal fun LedgerDateTimeDialog(
    initialTime: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    isTimeAllowed: (Long) -> Boolean = { true },
    invalidTimeMessage: String? = null,
) {
    val zone = remember { ZoneId.systemDefault() }
    val initial = remember(initialTime) { Instant.ofEpochMilli(initialTime).atZone(zone) }
    var day by rememberSaveable(initialTime) { mutableLongStateOf(initial.toLocalDate().toEpochDay()) }
    var hour by rememberSaveable(initialTime) { mutableStateOf(initial.hour.toString().padStart(2, '0')) }
    var minute by rememberSaveable(initialTime) { mutableStateOf(initial.minute.toString().padStart(2, '0')) }
    val date = LocalDate.ofEpochDay(day)
    val selectedTime = recordTimeMillis(date, hour, minute, zone)
    val allowedTime = selectedTime?.takeIf(isTimeAllowed)
    DialogWindowBase(onDismissRequest = onDismiss) {
        SurfaceBase(
            modifier = Modifier.fillMaxWidth().testTag("recordTimeDialog"),
            shape = LedgerStyle.shapes.dialog,
            color = LedgerStyle.colors.overlay, contentColor = LedgerStyle.colors.textPrimary,
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = LedgerStyle.dimensions.dialogMaxHeight).dialogContentPadding(LedgerStyle.spacing.extraLarge), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    LedgerDateTimePicker(date, hour, minute, { day = it.toEpochDay() }, { hour = it }, { minute = it })
                    if (invalidTimeMessage != null && selectedTime != null && allowedTime == null) {
                        Text(invalidTimeMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                DialogActions(
                    onCancel = onDismiss,
                    onConfirm = { allowedTime?.let(onConfirm) },
                    confirmEnabled = allowedTime != null,
                )
            }
        }
    }
}

/**
 * 将给定的日期、小时字符串与分钟字符串转换为指定时区的毫秒时间戳。
 *
 * @param date 目标日期 [LocalDate]。
 * @param hour 小时字符串（0-23）。
 * @param minute 分钟字符串（0-59）。
 * @param zone 时区 [ZoneId]。
 * @return 若输入的时间格式合法且在有效范围内，返回对应时间戳毫秒数；否则返回 null。
 */
internal fun recordTimeMillis(date: LocalDate, hour: String, minute: String, zone: ZoneId): Long? {
    val validHour = hour.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    val validMinute = minute.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    return date.atTime(validHour, validMinute).atZone(zone).toInstant().toEpochMilli()
}
