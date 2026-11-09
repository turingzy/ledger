package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon as ModelIcon

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showParseRulesDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    SettingsContent(
        uiState = uiState,
        onCategoryManagementClick = { showCategorySheet = true },
        onParseRulesClick = { showParseRulesDialog = true },
        onBackupClick = { showBackupDialog = true },
        onAboutClick = { showAboutDialog = true },
        modifier = modifier,
    )

    if (showCategorySheet) {
        val categories = (uiState as? SettingsUiState.Success)?.categories ?: emptyList()
        CategoryManagementSheet(
            categories = categories,
            onDismissRequest = { showCategorySheet = false },
            onAddCategoryClick = { showAddCategoryDialog = true },
            onDeleteCategory = { viewModel.deleteCategory(it) },
        )
    }

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

    if (showParseRulesDialog) {
        ParseRulesDialog(onDismissRequest = { showParseRulesDialog = false })
    }

    if (showBackupDialog) {
        BackupAndRestoreDialog(onDismissRequest = { showBackupDialog = false })
    }

    if (showAboutDialog) {
        val version = (uiState as? SettingsUiState.Success)?.appVersion ?: "1.0.0"
        AboutDialog(
            versionName = version,
            onDismissRequest = { showAboutDialog = false },
        )
    }
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onCategoryManagementClick: () -> Unit,
    onParseRulesClick: () -> Unit,
    onBackupClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) { innerPadding ->
        when (uiState) {
            SettingsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is SettingsUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item(key = "header_and_settings") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    text = "设置",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    letterSpacing = (-0.6).sp,
                                    modifier = Modifier.padding(bottom = 2.dp),
                                )
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(
                                        elevation = 2.dp,
                                        shape = RoundedCornerShape(16.dp),
                                        ambientColor = Color.Black.copy(alpha = 0.04f),
                                        spotColor = Color.Black.copy(alpha = 0.04f),
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    SettingsRowItem(
                                        icon = Icons.Default.CreditCard,
                                        iconBgColor = Color(0xFFFF9500),
                                        title = "分类管理",
                                        onClick = onCategoryManagementClick,
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 58.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                    SettingsRowItem(
                                        icon = Icons.Default.Code,
                                        iconBgColor = Color(0xFF5856D6),
                                        title = "账单解析规则",
                                        onClick = onParseRulesClick,
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 58.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                    SettingsRowItem(
                                        icon = Icons.Default.CloudUpload,
                                        iconBgColor = Color(0xFF34C759),
                                        title = "数据备份和恢复",
                                        onClick = onBackupClick,
                                    )
                                }
                            }
                        }
                    }

                    // 分组 2: 关于
                    item(key = "group_2") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 2.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = Color.Black.copy(alpha = 0.04f),
                                    spotColor = Color.Black.copy(alpha = 0.04f),
                                ),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                SettingsRowItem(
                                    icon = Icons.Default.Info,
                                    iconBgColor = Color(0xFF007AFF),
                                    title = "关于",
                                    onClick = onAboutClick,
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
fun SettingsRowItem(
    icon: ImageVector,
    iconBgColor: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(14.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementSheet(
    categories: List<CategoryWithIcon>,
    onDismissRequest: () -> Unit,
    onAddCategoryClick: () -> Unit,
    onDeleteCategory: (Long) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "分类管理",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                IconButton(onClick = onAddCategoryClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "添加分类",
                        tint = Color(0xFF007AFF),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp),
                ) {
                    items(
                        items = categories,
                        key = { it.category.id },
                    ) { categoryWithIcon ->
                        val cat = categoryWithIcon.category
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(
                                    text = cat.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = if (cat.type == CategoryType.EXPENSE) "支出分类" else "收入分类",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            if (!cat.isSystem) {
                                IconButton(onClick = { onDeleteCategory(cat.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "删除",
                                        tint = Color(0xFFFF3B30),
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            } else {
                                Text(
                                    text = "系统内置",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun ParseRulesDialog(
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF5856D6)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
        title = {
            Text(
                text = "账单解析规则",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "支持智能解析微信支付通知、支付宝账单、银行扣款短信。",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "• 自动提取交易金额、交易对手与商户分类\n• 本地离线正规匹配，零网络上传保护隐私\n• 智能对应至绑定的默认扣款卡账户",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("了解", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
fun BackupAndRestoreDialog(
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF34C759)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        },
        title = {
            Text(
                text = "数据备份和恢复",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "本地 SQLite 沙盒数据库状态良好。",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "所有记账明细、账户及分类数据仅存储在您的设备受保护存储中。您可以随时导出加密备份文件或进行数据迁移恢复。",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("完成", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold)
            }
        },
    )
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
                        icon = ModelIcon(id = 1L, keyName = "ic_food", displayName = "餐饮", iconUri = "res://"),
                    ),
                    CategoryWithIcon(
                        category = Category(id = 2L, name = "外设数码", type = CategoryType.EXPENSE, iconId = 1L, isSystem = false),
                        icon = ModelIcon(id = 1L, keyName = "ic_shopping", displayName = "数码", iconUri = "res://"),
                    ),
                ),
                availableIcons = emptyList(),
            ),
            onCategoryManagementClick = {},
            onParseRulesClick = {},
            onBackupClick = {},
            onAboutClick = {},
        )
    }
}
