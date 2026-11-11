package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import cn.turingzy.ledger.ui.component.LedgerIconButton as IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
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
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.ui.theme.IosYellow
import cn.turingzy.ledger.ui.theme.IosPurple
import cn.turingzy.ledger.ui.theme.IosOrange
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.util.toFormattedAmount
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
    val selectionColor = when (transactionType) {
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.error
        TransactionType.INCOME -> MaterialTheme.colorScheme.tertiary
        TransactionType.TRANSFER -> IosYellow
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(Modifier.height(height).testTag("recordAccountDrawer").navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                IconButton(showPressFeedback = false, onClick = onDismiss) {
                    Icon(MaterialSymbols.Close, "关闭账户选择", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
            if (accounts.isEmpty()) {
                Text("暂无可选账户，请先在钱包中添加", Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                items(accounts, key = { it.account.id }) { item ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onSelect(item.account.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        RecordAccountAvatar(item.account)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.account.name, style = MaterialTheme.typography.bodyMedium)
                            Text(item.account.currentBalance.toFormattedAmount(showSymbol = true), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (item.account.id == selectedId) Icon(MaterialSymbols.Check, "已选择", tint = selectionColor)
                    }
                }
            }
        }
    }
}

@Composable
internal fun RecordDateTimeDialog(initialTime: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val zone = remember { ZoneId.systemDefault() }
    val initial = remember(initialTime) { Instant.ofEpochMilli(initialTime).atZone(zone) }
    var day by rememberSaveable(initialTime) { mutableLongStateOf(initial.toLocalDate().toEpochDay()) }
    var hour by rememberSaveable(initialTime) { mutableStateOf(initial.hour.toString().padStart(2, '0')) }
    var minute by rememberSaveable(initialTime) { mutableStateOf(initial.minute.toString().padStart(2, '0')) }
    val date = LocalDate.ofEpochDay(day)
    val selectedTime = recordTimeMillis(date, hour, minute, zone)
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().testTag("recordTimeDialog"),
            shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    RecordDateTimePicker(date, hour, minute, { day = it.toEpochDay() }, { hour = it }, { minute = it })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(showPressFeedback = false, onClick = onDismiss) { Icon(MaterialSymbols.Close, "取消") }
                    Spacer(Modifier.weight(1f))
                    IconButton(showPressFeedback = false, onClick = { selectedTime?.let(onConfirm) }, enabled = selectedTime != null) {
                        Icon(MaterialSymbols.Check, "确认", tint = MaterialTheme.colorScheme.tertiary.copy(alpha = if (selectedTime != null) 1f else 0.38f))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordAccountAvatar(account: Account) {
    val colors = MaterialTheme.colorScheme
    val color = when {
        account.name.contains("支付宝") -> colors.primary
        account.name.contains("微信") -> colors.tertiary
        account.type == AccountType.DEBIT -> colors.primary
        account.type == AccountType.CREDIT -> IosPurple
        account.type == AccountType.CASH -> IosOrange
        else -> colors.tertiary
    }
    val icon = when (account.type) {
        AccountType.DEBIT -> MaterialSymbols.AccountBalance
        AccountType.CREDIT -> MaterialSymbols.Contactless
        AccountType.WALLET -> MaterialSymbols.AccountBalanceWallet
        AccountType.CASH -> MaterialSymbols.Payments
    }
    Box(Modifier.size(36.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(20.dp), tint = colors.onPrimary)
    }
}
