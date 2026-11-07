package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.AccountWithTemplate
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.util.AmountFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordTransactionSheet(
    sheetState: SheetState,
    categories: List<CategoryWithIcon>,
    accounts: List<AccountWithTemplate>,
    onDismissRequest: () -> Unit,
    onConfirm: (
        type: TransactionType,
        amount: Long,
        accountId: Long,
        toAccountId: Long?,
        categoryId: Long,
        note: String?,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    val filteredCategories = remember(categories, selectedType) {
        when (selectedType) {
            TransactionType.EXPENSE -> categories.filter { it.category.type == CategoryType.EXPENSE }
            TransactionType.INCOME -> categories.filter { it.category.type == CategoryType.INCOME }
            TransactionType.TRANSFER -> categories.filter { it.category.name.contains("转账") }
        }
    }

    var selectedCategoryId by remember(filteredCategories) {
        mutableStateOf(filteredCategories.firstOrNull()?.category?.id ?: 0L)
    }

    var selectedAccountId by remember(accounts) {
        mutableStateOf(accounts.firstOrNull()?.account?.id ?: 0L)
    }

    var toAccountId by remember(accounts) {
        mutableStateOf(accounts.getOrNull(1)?.account?.id ?: accounts.firstOrNull()?.account?.id ?: 0L)
    }

    var accountMenuExpanded by remember { mutableStateOf(false) }
    var toAccountMenuExpanded by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val isAmountValid = (AmountFormatter.parseToCents(amountText) ?: 0L) > 0L
    val canSubmit = isAmountValid && selectedAccountId != 0L && selectedCategoryId != 0L &&
        (selectedType != TransactionType.TRANSFER || selectedAccountId != toAccountId)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "记一笔",
                style = MaterialTheme.typography.titleLarge,
            )

            // 类型选择: 支出 / 收入 / 转账
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TransactionType.entries.forEach { type ->
                    val label = when (type) {
                        TransactionType.EXPENSE -> "支出"
                        TransactionType.INCOME -> "收入"
                        TransactionType.TRANSFER -> "转账"
                    }
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(label) },
                    )
                }
            }

            // 金额输入框
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("金额 (元)") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // 扣款/收入账户选择
            ExposedDropdownMenuBox(
                expanded = accountMenuExpanded,
                onExpandedChange = { accountMenuExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val currentAccountName = accounts.firstOrNull { it.account.id == selectedAccountId }?.account?.name ?: "请选择账户"
                OutlinedTextField(
                    value = currentAccountName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (selectedType == TransactionType.TRANSFER) "转出账户" else "账户") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = accountMenuExpanded,
                    onDismissRequest = { accountMenuExpanded = false },
                ) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = { Text(acc.account.name) },
                            onClick = {
                                selectedAccountId = acc.account.id
                                accountMenuExpanded = false
                            },
                        )
                    }
                }
            }

            // 若为转账，展示转入账户选择
            if (selectedType == TransactionType.TRANSFER) {
                ExposedDropdownMenuBox(
                    expanded = toAccountMenuExpanded,
                    onExpandedChange = { toAccountMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val targetAccountName = accounts.firstOrNull { it.account.id == toAccountId }?.account?.name ?: "请选择转入账户"
                    OutlinedTextField(
                        value = targetAccountName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("转入账户") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = toAccountMenuExpanded,
                        onDismissRequest = { toAccountMenuExpanded = false },
                    ) {
                        accounts.forEach { acc ->
                            DropdownMenuItem(
                                text = { Text(acc.account.name) },
                                onClick = {
                                    toAccountId = acc.account.id
                                    toAccountMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }

            // 分类选择
            ExposedDropdownMenuBox(
                expanded = categoryMenuExpanded,
                onExpandedChange = { categoryMenuExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                val currentCategoryName = filteredCategories.firstOrNull { it.category.id == selectedCategoryId }?.category?.name ?: "请选择分类"
                OutlinedTextField(
                    value = currentCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("分类") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )
                ExposedDropdownMenu(
                    expanded = categoryMenuExpanded,
                    onDismissRequest = { categoryMenuExpanded = false },
                ) {
                    filteredCategories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.category.name) },
                            onClick = {
                                selectedCategoryId = cat.category.id
                                categoryMenuExpanded = false
                            },
                        )
                    }
                }
            }

            // 备注输入框
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注 (可选)") },
                placeholder = { Text("例如：晚餐便当、周末聚餐") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = {
                    val amountCents = AmountFormatter.parseToCents(amountText) ?: 0L
                    onConfirm(
                        selectedType,
                        amountCents,
                        selectedAccountId,
                        if (selectedType == TransactionType.TRANSFER) toAccountId else null,
                        selectedCategoryId,
                        note,
                    )
                },
                enabled = canSubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text("保存")
            }
        }
    }
}
