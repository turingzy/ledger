package cn.turingzy.ledger.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate

@Composable
internal fun LedgerDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var day by rememberSaveable(initialDate) { mutableLongStateOf(initialDate.toEpochDay()) }
    val date = LocalDate.ofEpochDay(day)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier.fillMaxWidth().testTag("ledgerDatePickerDialog"),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    LedgerDatePicker(date) { day = it.toEpochDay() }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LedgerIconButton(onDismiss, showPressFeedback = false) { Icon(MaterialSymbols.Close, "取消") }
                    Spacer(Modifier.weight(1f))
                    LedgerIconButton({ onConfirm(date) }, showPressFeedback = false) {
                        Icon(MaterialSymbols.Check, "确认", tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}
