package cn.turingzy.ledger.ui.feature.settings

import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import cn.turingzy.ledger.ui.component.topBarPressFeedback
import cn.turingzy.ledger.navigation.SettingsDestination
import cn.turingzy.ledger.navigation.SettingsNavigation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import cn.turingzy.ledger.ui.component.LocalBottomBarContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    onDetailVisibleChange: (Boolean) -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var parseRulePreviews by remember { mutableStateOf(defaultParseRulePreviews()) }
    SettingsNavigation(
        onDetailVisibleChange = onDetailVisibleChange,
        modifier = modifier,
    ) { destination, navigate, back ->
        when (destination) {
            SettingsDestination.ROOT -> SettingsContent(
                uiState = uiState,
                onParseRulesClick = { navigate(SettingsDestination.PARSE_RULES) },
                onCardManagementClick = { navigate(SettingsDestination.CARDS) },
                onBillTransferClick = { navigate(SettingsDestination.BILL_TRANSFER) },
                onBackupClick = { navigate(SettingsDestination.BACKUP) },
                onAboutClick = { navigate(SettingsDestination.ABOUT) },
            )
            SettingsDestination.PARSE_RULES -> ParseRulesScreen(
                accounts = (uiState as? SettingsUiState.Success)?.accounts.orEmpty(),
                categories = (uiState as? SettingsUiState.Success)?.categories.orEmpty(),
                rules = parseRulePreviews,
                onSaveRule = { rule ->
                    parseRulePreviews = if (parseRulePreviews.any { it.id == rule.id }) {
                        parseRulePreviews.map { if (it.id == rule.id) rule else it }
                    } else parseRulePreviews + rule
                },
                onDeleteRule = { id -> parseRulePreviews = parseRulePreviews.filterNot { it.id == id } },
                onDismissRequest = back,
            )
            SettingsDestination.BACKUP -> BackupAndRestoreScreen(onDismissRequest = back)
            SettingsDestination.CARDS -> CardTemplateManagementScreen(
                templates = (uiState as? SettingsUiState.Success)?.cardTemplates.orEmpty(),
                onDismissRequest = back,
            )
            SettingsDestination.BILL_TRANSFER -> BillTransferScreen(rules = parseRulePreviews, onDismissRequest = back)
            SettingsDestination.ABOUT -> AboutScreen(
                versionName = (uiState as? SettingsUiState.Success)?.appVersion ?: "1.0.0",
                onDismissRequest = back,
                onOpenSourceClick = { navigate(SettingsDestination.OPEN_SOURCE_LICENSES) },
            )
            SettingsDestination.OPEN_SOURCE_LICENSES -> OpenSourceLibrariesScreen(onBack = back)
        }
    }
}

@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onParseRulesClick: () -> Unit,
    onCardManagementClick: () -> Unit,
    onBillTransferClick: () -> Unit,
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp + innerPadding.calculateBottomPadding() + LocalBottomBarContentPadding.current),
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
                                        icon = MaterialSymbols.DashboardCustomize,
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        title = "卡面管理",
                                        onClick = onCardManagementClick,
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 58.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                    SettingsRowItem(
                                        icon = MaterialSymbols.Code,
                                        iconColor = MaterialTheme.colorScheme.secondary,
                                        title = "账单解析规则",
                                        onClick = onParseRulesClick,
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 58.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant,
                                    )
                                    SettingsRowItem(
                                        icon = MaterialSymbols.ImportExport,
                                        iconColor = MaterialTheme.colorScheme.tertiary,
                                        title = "账单导入与导出",
                                        onClick = onBillTransferClick,
                                    )
                                }
                            }
                        }
                    }

                    item(key = "backup_and_about") {
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
                                    icon = MaterialSymbols.CloudUpload,
                                    iconColor = MaterialTheme.colorScheme.tertiary,
                                    title = "备份与恢复",
                                    onClick = onBackupClick,
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 58.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                                SettingsRowItem(
                                    icon = MaterialSymbols.Info,
                                    iconColor = MaterialTheme.colorScheme.primary,
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
    iconColor: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .topBarPressFeedback(interactionSource, enabled = true)
            .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp),
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
            imageVector = MaterialSymbols.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(24.dp),
        )
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
                        icon = ModelIcon(id = 1L, keyName = "ic_food", displayName = "餐饮", iconUri = "res://"),
                    ),
                    CategoryWithIcon(
                        category = Category(id = 2L, name = "外设数码", type = CategoryType.EXPENSE, iconId = 1L, isSystem = false),
                        icon = ModelIcon(id = 1L, keyName = "ic_shopping", displayName = "数码", iconUri = "res://"),
                    ),
                ),
            ),
            onParseRulesClick = {},
            onCardManagementClick = {},
            onBillTransferClick = {},
            onBackupClick = {},
            onAboutClick = {},
        )
    }
}
