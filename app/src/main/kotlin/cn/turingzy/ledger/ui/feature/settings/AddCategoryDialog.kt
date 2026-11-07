package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.Icon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCategoryDialog(
    availableIcons: List<Icon>,
    onDismissRequest: () -> Unit,
    onConfirm: (name: String, type: CategoryType, iconId: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CategoryType.EXPENSE) }
    var selectedIconId by remember(availableIcons) {
        mutableStateOf(availableIcons.firstOrNull()?.id ?: 1L)
    }
    var iconMenuExpanded by remember { mutableStateOf(false) }

    val isNameValid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("添加分类") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    placeholder = { Text("例如：数码配件") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(
                    text = "分类类型",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                ) {
                    listOf(
                        CategoryType.EXPENSE to "支出分类",
                        CategoryType.INCOME to "收入分类",
                    ).forEach { (type, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = null,
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }

                // 关联图标选择
                ExposedDropdownMenuBox(
                    expanded = iconMenuExpanded,
                    onExpandedChange = { iconMenuExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val currentIconName = availableIcons.firstOrNull { it.id == selectedIconId }?.displayName ?: "系统图标"
                    OutlinedTextField(
                        value = currentIconName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("关联图标") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = iconMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(
                        expanded = iconMenuExpanded,
                        onDismissRequest = { iconMenuExpanded = false },
                    ) {
                        availableIcons.forEach { icon ->
                            DropdownMenuItem(
                                text = { Text(icon.displayName) },
                                onClick = {
                                    selectedIconId = icon.id
                                    iconMenuExpanded = false
                                },
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isNameValid) {
                        onConfirm(name, selectedType, selectedIconId)
                    }
                },
                enabled = isNameValid,
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        },
        modifier = modifier,
    )
}
