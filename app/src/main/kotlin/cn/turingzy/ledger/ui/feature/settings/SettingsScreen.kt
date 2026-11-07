package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    SettingsContent(
        uiState = uiState,
        onAddCategoryClick = { showAddCategoryDialog = true },
        onDeleteCategoryClick = { viewModel.deleteCategory(it) },
        onAboutClick = { showAboutDialog = true },
        modifier = modifier,
    )

    if (showAddCategoryDialog) {
        val icons = (uiState as? SettingsUiState.Success)?.availableIcons ?: emptyList()
        AddCategoryDialog(
            availableIcons = icons,
            onDismissRequest = { showAddCategoryDialog = false },
            onConfirm = { name, type, iconId ->
                viewModel.createCategory(name, type, iconId)
                showAddCategoryDialog = false
            },
        )
    }

    if (showAboutDialog) {
        val version = (uiState as? SettingsUiState.Success)?.appVersion ?: "1.0.0"
        AboutDialog(
            versionName = version,
            onDismissRequest = { showAboutDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onAddCategoryClick: () -> Unit,
    onDeleteCategoryClick: (Long) -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("系统设置") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { innerPadding ->
        when (uiState) {
            SettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is SettingsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 常规设置组: 存储信息与关于
                    item(key = "general_section") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            ),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Storage,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(text = "数据存储")
                                    }
                                    Text(
                                        text = "本地 SQLite 沙盒",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAboutClick() }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Text(text = "关于 Ledger")
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                                        contentDescription = "查看关于",
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }

                    // 分类管理组头部
                    item(key = "category_section_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "分类管理 (${uiState.categories.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            IconButton(onClick = onAddCategoryClick) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "添加新分类",
                                )
                            }
                        }
                    }

                    // 分类列表
                    items(
                        items = uiState.categories,
                        key = { it.category.id },
                    ) { categoryWithIcon ->
                        val cat = categoryWithIcon.category
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = cat.name,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp),
                                    )
                                    Column {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                        )
                                        Text(
                                            text = if (cat.type == CategoryType.EXPENSE) "支出分类" else "收入分类",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                if (!cat.isSystem) {
                                    IconButton(onClick = { onDeleteCategoryClick(cat.id) }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "删除分类",
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "内置",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(end = 8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsContentPreview() {
    MaterialTheme {
        SettingsContent(
            uiState = SettingsUiState.Success(
                categories = listOf(
                    CategoryWithIcon(
                        category = Category(id = 1L, name = "餐饮食品", type = CategoryType.EXPENSE, iconId = 1L, isSystem = true),
                        icon = Icon(id = 1L, keyName = "ic_food", displayName = "餐饮", iconUri = "res://"),
                    ),
                    CategoryWithIcon(
                        category = Category(id = 2L, name = "外设数码", type = CategoryType.EXPENSE, iconId = 1L, isSystem = false),
                        icon = Icon(id = 1L, keyName = "ic_shopping", displayName = "数码", iconUri = "res://"),
                    ),
                ),
                availableIcons = emptyList(),
            ),
            onAddCategoryClick = {},
            onDeleteCategoryClick = {},
            onAboutClick = {},
        )
    }
}
