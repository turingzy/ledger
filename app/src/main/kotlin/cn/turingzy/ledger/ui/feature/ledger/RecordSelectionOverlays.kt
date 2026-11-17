package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.ui.components.base.BaseIconButtonContent
import cn.turingzy.ledger.ui.components.base.BaseDivider
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.components.base.BaseSurface
import cn.turingzy.ledger.ui.components.base.BaseDialogWindow
import cn.turingzy.ledger.ui.components.base.BaseModalBottomSheet
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.ui.component.LedgerDateTimePicker
import cn.turingzy.ledger.ui.component.MaterialSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RecordAccountSheet(
    title: String,
    accounts: List<AccountWithTemplate>,
    selectedId: Long,
    transactionType: TransactionType,
    height: Dp,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val selectionColor = LedgerStyle.colors.transaction(transactionType)
    BaseModalBottomSheet(
        onDismissRequest = onDismiss,
        contentHandlesInsets = true,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(Modifier.height(height).testTag("recordAccountDrawer").navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                BaseIconButtonContent(showPressFeedback = false, onClick = onDismiss) {
                    Icon(MaterialSymbols.Close, textResources.getString(cn.turingzy.ledger.R.string.ui_close_account_picker), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            BaseDivider()
            if (accounts.isEmpty()) {
                Text(textResources.getString(cn.turingzy.ledger.R.string.ui_no_accounts_available_add_one_in_wallet), Modifier.padding(LedgerStyle.spacing.extraLarge), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(accounts, key = { it.account.id }) { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(item.account.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.large),
                    ) {
                        cn.turingzy.ledger.ui.components.business.BusinessAccountIcon(item.account)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.tiny)) {
                            Text(item.account.name, style = MaterialTheme.typography.bodyMedium)
                            cn.turingzy.ledger.ui.components.business.BusinessAmountText(
                                item.account.currentBalance, item.account.currency,
                                textStyle = MaterialTheme.typography.bodySmall,
                                contentColor = LedgerStyle.colors.textSecondary,
                            )
                        }
                        if (item.account.id == selectedId) Icon(MaterialSymbols.Check, textResources.getString(cn.turingzy.ledger.R.string.ui_selected), tint = selectionColor)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RecordDateTimeDialog(initialTime: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val zone = remember { ZoneId.systemDefault() }
    val initial = remember(initialTime) { Instant.ofEpochMilli(initialTime).atZone(zone) }
    var day by rememberSaveable(initialTime) { mutableLongStateOf(initial.toLocalDate().toEpochDay()) }
    var hour by rememberSaveable(initialTime) { mutableStateOf(initial.hour.toString().padStart(2, '0')) }
    var minute by rememberSaveable(initialTime) { mutableStateOf(initial.minute.toString().padStart(2, '0')) }
    val date = LocalDate.ofEpochDay(day)
    val selectedTime = recordTimeMillis(date, hour, minute, zone)
    BaseDialogWindow(onDismissRequest = onDismiss) {
        BaseSurface(
            modifier = Modifier.fillMaxWidth().testTag("recordTimeDialog"),
            shape = LedgerStyle.shapes.dialog, color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = LedgerStyle.dimensions.dialogMaxHeight).padding(LedgerStyle.spacing.extraLarge), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    LedgerDateTimePicker(date, hour, minute, { day = it.toEpochDay() }, { hour = it }, { minute = it })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BaseIconButtonContent(showPressFeedback = false, onClick = onDismiss) { Icon(MaterialSymbols.Close, textResources.getString(cn.turingzy.ledger.R.string.ui_cancel)) }
                    Spacer(Modifier.weight(1f))
                    BaseIconButtonContent(showPressFeedback = false, onClick = { selectedTime?.let(onConfirm) }, enabled = selectedTime != null) {
                        Icon(MaterialSymbols.Check, textResources.getString(cn.turingzy.ledger.R.string.ui_confirm), tint = MaterialTheme.colorScheme.tertiary.copy(alpha = if (selectedTime != null) 1f else LedgerStyle.opacity.disabled))
                    }
                }
            }
        }
    }
}
