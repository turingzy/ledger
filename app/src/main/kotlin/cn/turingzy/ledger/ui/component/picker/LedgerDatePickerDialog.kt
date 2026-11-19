package cn.turingzy.ledger.ui.component.picker

import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.IconButtonBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.component.base.DialogWindowBase
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import java.time.LocalDate

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
            shape = LedgerStyle.shapes.dialog, color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                Modifier.fillMaxWidth().heightIn(max = LedgerStyle.dimensions.dialogMaxHeight).padding(LedgerStyle.spacing.extraLarge),
                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
            ) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    LedgerDatePicker(date) { day = it.toEpochDay() }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButtonBase(MaterialSymbols.Close, textResources.getString(cn.turingzy.ledger.R.string.ui_cancel),
                        onDismiss, showPressFeedback = false)
                    Spacer(Modifier.weight(1f))
                    IconButtonBase(MaterialSymbols.Check, textResources.getString(cn.turingzy.ledger.R.string.ui_confirm),
                        { onConfirm(date) }, contentColor = MaterialTheme.colorScheme.tertiary, showPressFeedback = false)
                }
            }
        }
    }
}
