package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.model.Transaction
import cn.turingzy.ledger.model.TransactionDetail
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.util.toFormattedAmount
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    showRecordSheet: Boolean,
    onRecordSheetDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LedgerViewModel = hiltViewModel(),
    onNavigateToRecord: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LedgerContent(
        uiState = uiState,
        onDeleteTransactionClick = { txId -> viewModel.deleteTransaction(txId) },
        modifier = modifier,
    )

    if (showRecordSheet) {
        RecordTransactionSheet(
            sheetState = sheetState,
            categories = categories,
            accounts = accounts,
            onDismissRequest = onRecordSheetDismiss,
            onConfirm = { type, amount, accountId, toAccountId, categoryId, note ->
                viewModel.createTransaction(type, amount, accountId, toAccountId, categoryId, note)
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onRecordSheetDismiss()
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerContent(
    uiState: LedgerUiState,
    onDeleteTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("账本") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { innerPadding ->
        when (uiState) {
            LedgerUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is LedgerUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "month_summary_card") {
                        MonthSummaryCard(
                            monthDisplay = uiState.monthDisplay,
                            income = uiState.monthIncome,
                            expense = uiState.monthExpense,
                            balance = uiState.monthBalance,
                        )
                    }

                    if (uiState.dayGroups.isEmpty()) {
                        item(key = "empty_placeholder") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "本月暂无交易明细，点击右下角 '记一笔' 开始记账",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        uiState.dayGroups.forEach { dayGroup ->
                            item(key = "header_${dayGroup.dateEpochDay}") {
                                DayGroupHeader(
                                    dateText = dayGroup.displayDate,
                                    expense = dayGroup.totalExpense,
                                    income = dayGroup.totalIncome,
                                )
                            }

                            items(
                                items = dayGroup.transactions,
                                key = { it.transaction.id },
                            ) { detail ->
                                TransactionItemCard(
                                    detail = detail,
                                    onDeleteClick = { onDeleteTransactionClick(detail.transaction.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthSummaryCard(
    monthDisplay: String,
    income: Long,
    expense: Long,
    balance: Long,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = "$monthDisplay 结余",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = balance.toFormattedAmount(showSymbol = true, useGrouping = true),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = expense.toFormattedAmount(showSymbol = true),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "收入",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = income.toFormattedAmount(showSymbol = true),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
fun DayGroupHeader(
    dateText: String,
    expense: Long,
    income: Long,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dateText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        val parts = mutableListOf<String>()
        if (expense > 0) parts.add("支: ${expense.toFormattedAmount(showSymbol = true)}")
        if (income > 0) parts.add("收: ${income.toFormattedAmount(showSymbol = true)}")

        if (parts.isNotEmpty()) {
            Text(
                text = parts.joinToString("  "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    detail: TransactionDetail,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tx = detail.transaction
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val timeStr = remember(tx.transactionTime) {
        Instant.ofEpochMilli(tx.transactionTime).atZone(ZoneId.systemDefault()).format(timeFormatter)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Payments,
                    contentDescription = detail.category.category.name,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )

                Column {
                    Text(
                        text = tx.note?.takeIf { it.isNotBlank() } ?: detail.category.category.name,
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    val subInfo = buildString {
                        append(detail.category.category.name)
                        append(" · ")
                        append(detail.account.name)
                        if (tx.type == TransactionType.TRANSFER && detail.toAccount != null) {
                            append(" -> ${detail.toAccount.name}")
                        }
                    }

                    Text(
                        text = subInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    val amountPrefix = when (tx.type) {
                        TransactionType.EXPENSE -> "-"
                        TransactionType.INCOME -> "+"
                        TransactionType.TRANSFER -> ""
                    }
                    val amountColor = when (tx.type) {
                        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
                        TransactionType.INCOME -> MaterialTheme.colorScheme.primary
                        TransactionType.TRANSFER -> MaterialTheme.colorScheme.outline
                    }

                    Text(
                        text = "$amountPrefix${tx.amount.toFormattedAmount(showSymbol = true)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = amountColor,
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "删除记录",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LedgerContentPreview() {
    MaterialTheme {
        LedgerContent(
            uiState = LedgerUiState.Success(
                monthDisplay = "2026年11月",
                monthIncome = 2400000L,
                monthExpense = 1155000L,
                monthBalance = 1245000L,
                dayGroups = listOf(
                    DayTransactionGroup(
                        dateEpochDay = 20000L,
                        displayDate = "今天 · 11月1日 周日",
                        totalExpense = 18600L,
                        totalIncome = 0L,
                        transactions = listOf(
                            TransactionDetail(
                                transaction = Transaction(
                                    id = 1L,
                                    type = TransactionType.EXPENSE,
                                    amount = 14200L,
                                    accountId = 1L,
                                    categoryId = 1L,
                                    transactionTime = System.currentTimeMillis(),
                                    note = "盒马鲜生采买",
                                ),
                                account = Account(id = 1L, name = "支付宝", type = AccountType.WALLET),
                                category = CategoryWithIcon(
                                    category = Category(id = 1L, name = "餐饮食品", type = CategoryType.EXPENSE, iconId = 1L),
                                    icon = Icon(id = 1L, keyName = "ic_food", displayName = "餐饮", iconUri = "res://"),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
            onDeleteTransactionClick = {},
        )
    }
}
