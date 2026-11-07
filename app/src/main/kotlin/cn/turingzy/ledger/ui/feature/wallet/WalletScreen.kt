package cn.turingzy.ledger.ui.feature.wallet

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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Wallet
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.Account
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
fun WalletScreen(
    modifier: Modifier = Modifier,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToAdjust by remember { mutableStateOf<Account?>(null) }

    WalletContent(
        uiState = uiState,
        onAddAccountClick = { showAddDialog = true },
        onAdjustBalanceClick = { account -> accountToAdjust = account },
        onDeleteAccountClick = { accountId -> viewModel.deleteAccount(accountId) },
        modifier = modifier,
    )

    if (showAddDialog) {
        AddAccountDialog(
            onDismissRequest = { showAddDialog = false },
            onConfirm = { name, type, balance, cardNo, includeInTotal ->
                viewModel.createAccount(name, type, balance, cardNo, includeInTotal)
                showAddDialog = false
            },
        )
    }

    accountToAdjust?.let { account ->
        AdjustBalanceDialog(
            account = account,
            onDismissRequest = { accountToAdjust = null },
            onConfirm = { newBalance ->
                viewModel.adjustBalance(account.id, newBalance)
                accountToAdjust = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletContent(
    uiState: WalletUiState,
    onAddAccountClick: () -> Unit,
    onAdjustBalanceClick: (Account) -> Unit,
    onDeleteAccountClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("钱包与资产") },
                actions = {
                    IconButton(onClick = onAddAccountClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加账户",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { innerPadding ->
        when (uiState) {
            WalletUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is WalletUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "header_summary") {
                        TotalAssetSummaryCard(
                            totalAsset = uiState.totalAsset,
                            totalDebt = uiState.totalDebt,
                        )
                    }

                    item(key = "section_title") {
                        Text(
                            text = "账户列表 (${uiState.accounts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }

                    if (uiState.accounts.isEmpty()) {
                        item(key = "empty_placeholder") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = "暂无账户，点击右上角 '+' 添加",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    } else {
                        items(
                            items = uiState.accounts,
                            key = { it.account.id },
                        ) { accountWithTemplate ->
                            AccountItemCard(
                                item = accountWithTemplate,
                                onAdjustClick = { onAdjustBalanceClick(accountWithTemplate.account) },
                                onDeleteClick = { onDeleteAccountClick(accountWithTemplate.account.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TotalAssetSummaryCard(
    totalAsset: Long,
    totalDebt: Long,
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
                text = "净资产",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = totalAsset.toFormattedAmount(showSymbol = true, useGrouping = true),
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
                        text = "总计入资产",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                    Text(
                        text = totalAsset.toFormattedAmount(showSymbol = true),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                if (totalDebt != 0L) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "信用卡应还/负债",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                        Text(
                            text = totalDebt.toFormattedAmount(showSymbol = true),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountItemCard(
    item: AccountWithTemplate,
    onAdjustClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val account = item.account

    val icon: ImageVector = when (account.type) {
        AccountType.DEBIT -> Icons.Default.AccountBalance
        AccountType.CREDIT -> Icons.Default.CreditCard
        AccountType.WALLET -> Icons.Default.Wallet
        AccountType.CASH -> Icons.Default.Payments
    }

    val typeLabel = when (account.type) {
        AccountType.DEBIT -> "储蓄卡"
        AccountType.CREDIT -> "信用卡"
        AccountType.WALLET -> "电子钱包"
        AccountType.CASH -> "现金"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = typeLabel,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp),
                )

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = account.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (!account.cardNumber.isNullOrEmpty()) {
                            Text(
                                text = "(*${account.cardNumber})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        text = if (account.includeInTotal) typeLabel else "$typeLabel · 不计入总资产",
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
                    Text(
                        text = account.currentBalance.toFormattedAmount(showSymbol = true, useGrouping = true),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (account.type == AccountType.CREDIT && account.currentBalance < 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                    )
                }

                IconButton(onClick = onAdjustClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "调整余额",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }

                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "删除账户",
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
fun WalletContentPreview() {
    MaterialTheme {
        WalletContent(
            uiState = WalletUiState.Success(
                totalAsset = 1588000L,
                totalDebt = 240000L,
                accounts = listOf(
                    AccountWithTemplate(
                        account = Account(
                            id = 1L,
                            name = "招商银行储蓄卡",
                            type = AccountType.DEBIT,
                            currentBalance = 1200000L,
                            cardNumber = "6821",
                        ),
                    ),
                    AccountWithTemplate(
                        account = Account(
                            id = 2L,
                            name = "微信零钱",
                            type = AccountType.WALLET,
                            currentBalance = 388000L,
                        ),
                    ),
                ),
            ),
            onAddAccountClick = {},
            onAdjustBalanceClick = {},
            onDeleteAccountClick = {},
        )
    }
}
