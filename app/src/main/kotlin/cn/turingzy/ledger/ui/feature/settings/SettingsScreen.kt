package cn.turingzy.ledger.ui.feature.settings

import cn.turingzy.ledger.ui.feature.settings.screen.AboutScreen
import cn.turingzy.ledger.ui.feature.settings.screen.BillTransferScreen
import cn.turingzy.ledger.ui.feature.settings.screen.CardTemplateManagementScreen
import cn.turingzy.ledger.ui.feature.settings.screen.GeneralSettingsScreen
import cn.turingzy.ledger.ui.feature.settings.screen.OpenSourceLibrariesScreen
import cn.turingzy.ledger.ui.feature.settings.screen.ParseRulesScreen
import cn.turingzy.ledger.ui.component.base.BaseAlertDialog
import cn.turingzy.ledger.ui.component.base.ScrollTitleContainerBase
import androidx.compose.foundation.lazy.rememberLazyListState
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.navigation.SettingsDestination
import cn.turingzy.ledger.navigation.SettingsNavigation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import androidx.compose.foundation.layout.ColumnScope
import cn.turingzy.ledger.ui.component.base.PageFrameBase
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.component.base.DividerBase
import cn.turingzy.ledger.ui.component.base.ListRowBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cn.turingzy.ledger.ui.AppStateViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 设置功能的主入口屏幕。
 *
 * 负责协调设置根列表与各子页面（通用设置、解析规则、卡片模板、账单导入导出、关于等）之间的导航切换，
 * 并处理全局消息对话框的展示与消除。
 *
 * @param modifier 修饰符
 * @param viewModel 设置界面的 ViewModel
 * @param onSubpageChange 进入或离开子页面时的回调，用于通知外层控制底部导航栏显隐等
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
    onSubpageChange: (Boolean) -> Unit = {},
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val appStateViewModel: AppStateViewModel = hiltViewModel()

    val parseRulePreviews = (uiState as? SettingsUiState.Success)?.rules.orEmpty()
    val message = (uiState as? SettingsUiState.Success)?.message
    if (message != null) BaseAlertDialog(
        onDismissRequest = viewModel::dismissMessage,
        text = { Text(message.orEmpty()) },
        onConfirm = viewModel::dismissMessage,
        showCancel = false,
        confirmLabel = textResources.getString(cn.turingzy.ledger.R.string.ui_ok),
    )
    SettingsNavigation(
        modifier = modifier,
        onSubpageChange = onSubpageChange,
    ) { destination, navigate, back ->
        when (destination) {
            SettingsDestination.ROOT -> SettingsContent(
                uiState = uiState,
                onGeneralSettingsClick = { navigate(SettingsDestination.GENERAL) },
                onParseRulesClick = { navigate(SettingsDestination.PARSE_RULES) },
                onCardManagementClick = { navigate(SettingsDestination.CARDS) },
                onBillTransferClick = { navigate(SettingsDestination.BILL_TRANSFER) },
                onAboutClick = { navigate(SettingsDestination.ABOUT) },
            )
            SettingsDestination.GENERAL -> GeneralSettingsScreen(onBack = back, viewModel = appStateViewModel)
            SettingsDestination.PARSE_RULES -> ParseRulesScreen(
                accounts = (uiState as? SettingsUiState.Success)?.accounts.orEmpty(),
                categories = (uiState as? SettingsUiState.Success)?.categories.orEmpty(),
                rules = parseRulePreviews,
                onSaveRule = viewModel::saveRule,
                onDeleteRule = viewModel::deleteRule,
                onDismissRequest = back,
            )
            SettingsDestination.CARDS -> (uiState as? SettingsUiState.Success)?.let { settings ->
                CardTemplateManagementScreen(
                    templates = settings.cardTemplates,
                    onCreate = viewModel::createTemplate,
                    onUpdate = viewModel::updateTemplate,
                    onDelete = viewModel::deleteTemplate,
                    onCopyImage = viewModel::copyImage,
                    onDismissRequest = back,
                )
            }
            SettingsDestination.BILL_TRANSFER -> BillTransferScreen(rules = parseRulePreviews, onDismissRequest = back,
                accounts = (uiState as? SettingsUiState.Success)?.accounts.orEmpty(),
                onPreviewImport = viewModel::previewImport, onImport = viewModel::importBills, onExport = viewModel::exportBills)
            SettingsDestination.ABOUT -> AboutScreen(
                versionName = (uiState as? SettingsUiState.Success)?.appVersion ?: "1.0.0",
                onDismissRequest = back,
                onOpenSourceClick = { navigate(SettingsDestination.OPEN_SOURCE_LICENSES) },
                onCheckLatest = viewModel::checkLatest,
            )
            SettingsDestination.OPEN_SOURCE_LICENSES -> OpenSourceLibrariesScreen(onBack = back)
        }
    }
}

/**
 * 设置主页的内容展示组件。
 *
 * 呈现分组式的设置项列表，包括通用设置、卡片模板、解析规则、账单导入导出以及关于软件等功能入口。
 *
 * @param uiState 当前设置界面的 UI 状态
 * @param onParseRulesClick 点击解析规则入口的回调
 * @param onCardManagementClick 点击卡片模板管理入口的回调
 * @param onBillTransferClick 点击账单导入导出入口的回调
 * @param onAboutClick 点击关于页面入口的回调
 * @param modifier 修饰符
 * @param onGeneralSettingsClick 点击通用设置入口的回调
 */
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onParseRulesClick: () -> Unit,
    onCardManagementClick: () -> Unit,
    onBillTransferClick: () -> Unit,
    onAboutClick: () -> Unit,
    modifier: Modifier = Modifier,
    onGeneralSettingsClick: () -> Unit = {},
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    PageFrameBase(modifier) {
        when (uiState) {
            SettingsUiState.Loading -> Unit
            is SettingsUiState.Success -> {
                val listState = rememberLazyListState()
                ScrollTitleContainerBase(
                    title = textResources.getString(cn.turingzy.ledger.R.string.ui_settings),
                    scrollOffset = {
                        if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset
                        else Int.MAX_VALUE
                    },
                    onScrollToTop = { listState.animateScrollToItem(0) },
                ) { scrollModifier ->
                    LazyColumn(
                        state = listState,
                        modifier = scrollModifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = LedgerStyle.spacing.pageHorizontal, end = LedgerStyle.spacing.pageHorizontal,
                            top = LedgerStyle.spacing.extraSmall,
                            bottom = LedgerStyle.spacing.block + LocalBottomBarContentPadding.current,
                        ),
                        verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.sectionGap),
                    ) {
                        item(key = "header_and_settings") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.labelGap),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    contentAlignment = Alignment.CenterStart,
                                ) {
                                    Text(
                                        text = textResources.getString(cn.turingzy.ledger.R.string.ui_settings),
                                        fontSize = LedgerStyle.typography.screenTitle.fontSize,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        letterSpacing = (-0.6).sp,
                                        modifier = Modifier.padding(bottom = 2.dp),
                                    )
                                }

                                SettingsGroup {
                                    SettingsRowItem(
                                        icon = MaterialSymbols.Tune,
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        title = androidx.compose.ui.res.stringResource(cn.turingzy.ledger.R.string.general_settings),
                                        onClick = onGeneralSettingsClick,
                                    )
                                    DividerBase(Modifier.padding(start = LedgerStyle.dimensions.settingsDividerInset, end = LedgerStyle.spacing.rowHorizontal))
                                    SettingsRowItem(
                                        icon = MaterialSymbols.DashboardCustomize,
                                        iconColor = MaterialTheme.colorScheme.primary,
                                        title = textResources.getString(cn.turingzy.ledger.R.string.ui_card_designs),
                                        onClick = onCardManagementClick,
                                    )
                                    DividerBase(Modifier.padding(start = LedgerStyle.dimensions.settingsDividerInset, end = LedgerStyle.spacing.rowHorizontal))
                                    SettingsRowItem(
                                        icon = MaterialSymbols.Code,
                                        iconColor = MaterialTheme.colorScheme.secondary,
                                        title = textResources.getString(cn.turingzy.ledger.R.string.ui_import_rules),
                                        onClick = onParseRulesClick,
                                    )
                                    DividerBase(Modifier.padding(start = LedgerStyle.dimensions.settingsDividerInset, end = LedgerStyle.spacing.rowHorizontal))
                                    SettingsRowItem(
                                        icon = MaterialSymbols.ImportExport,
                                        iconColor = MaterialTheme.colorScheme.tertiary,
                                        title = textResources.getString(cn.turingzy.ledger.R.string.ui_import_and_export),
                                        onClick = onBillTransferClick,
                                    )
                                }
                            }
                        }

                        item(key = "about") {
                            SettingsGroup {
                                SettingsRowItem(
                                    icon = MaterialSymbols.Info,
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    title = textResources.getString(cn.turingzy.ledger.R.string.ui_about),
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

/**
 * 设置项分组卡片容器。
 *
 * 为内部的一组设置行提供圆角卡片底板。
 *
 * @param content 卡片内承载的列表项内容
 */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    CardBase(
        contentPadding = PaddingValues(LedgerStyle.spacing.none),
        contentSpacing = LedgerStyle.spacing.none,
        content = content,
    )
}

/**
 * 设置列表中的行项目组件。
 *
 * 显示左侧图标、设置项标题以及右侧的导航箭头。
 *
 * @param icon 设置项图标矢量图
 * @param iconColor 图标颜色
 * @param title 设置项标题文字
 * @param onClick 点击事件回调
 * @param modifier 修饰符
 */
@Composable
fun SettingsRowItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListRowBase(
        headline = { Text(title, style = LedgerStyle.typography.settingsRowTitle) },
        modifier = modifier,
        leadingSpacing = LedgerStyle.spacing.settingsIconGap,
        leading = {
            Box(Modifier.size(LedgerStyle.dimensions.listIconContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(LedgerStyle.icons.inline), tint = iconColor)
            }
        },
        trailing = {
            Icon(MaterialSymbols.ChevronRight, null, Modifier.size(LedgerStyle.icons.inline),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        },
        onClick = onClick,
    )
}
