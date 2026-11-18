package cn.turingzy.ledger.ui.component.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.BaseDialogWindow
import cn.turingzy.ledger.ui.component.base.BaseIconButton
import cn.turingzy.ledger.ui.component.base.BaseSurface
import cn.turingzy.ledger.ui.theme.LedgerStyle
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

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
    BaseDialogWindow(onDismissRequest = onDismiss) {
        BaseSurface(
            modifier = Modifier.fillMaxWidth().testTag("recordTimeDialog"),
            shape = LedgerStyle.shapes.dialog, color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = LedgerStyle.dimensions.dialogMaxHeight).padding(LedgerStyle.spacing.extraLarge), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    LedgerDateTimePicker(date, hour, minute, { day = it.toEpochDay() }, { hour = it }, { minute = it })
                    if (invalidTimeMessage != null && selectedTime != null && allowedTime == null) {
                        Text(invalidTimeMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BaseIconButton(MaterialSymbols.Close, stringResource(R.string.ui_cancel), onDismiss, showPressFeedback = false)
                    Spacer(Modifier.weight(1f))
                    BaseIconButton(MaterialSymbols.Check, stringResource(R.string.ui_confirm), { allowedTime?.let(onConfirm) },
                        enabled = allowedTime != null, contentColor = MaterialTheme.colorScheme.tertiary, showPressFeedback = false)
                }
            }
        }
    }
}

internal fun recordTimeMillis(date: LocalDate, hour: String, minute: String, zone: ZoneId): Long? {
    val validHour = hour.toIntOrNull()?.takeIf { it in 0..23 } ?: return null
    val validMinute = minute.toIntOrNull()?.takeIf { it in 0..59 } ?: return null
    return date.atTime(validHour, validMinute).atZone(zone).toInstant().toEpochMilli()
}
