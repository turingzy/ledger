package cn.turingzy.ledger.ui.feature.settings.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.data.repository.settings.AppUpdateRepository
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.base.BaseAlertDialog
import cn.turingzy.ledger.ui.component.base.CardBase
import cn.turingzy.ledger.ui.component.base.ListRowBase
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.feature.settings.component.SettingsActionRow
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageScroll
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.theme.LedgerTheme
import kotlinx.coroutines.CancellationException
import timber.log.Timber

/**
 * 版本更新检测状态枚举。
 */
private enum class UpdateCheckState {
    /** 空闲状态。 */
    IDLE,
    /** 正在检测更新。 */
    CHECKING,
    /** 检测到有新版本可用。 */
    AVAILABLE,
    /** 已确认当前版本是最新版本。 */
    UP_TO_DATE,
}

/**
 * “关于”页面屏幕组件。
 *
 * 展示应用名称、图标、当前安装版本、作者信息、应用简介，
 * 并支持在线检测 GitHub 最新版本以及跳转查看开源协议与源代码仓库。
 *
 * @param versionName 当前应用版本名称
 * @param onDismissRequest 返回或退出页面的回调
 * @param modifier 修饰符
 * @param onOpenSourceClick 跳转至开源库许可列表的回调
 * @param onCheckLatest 异步查询最新发布版本的挂起函数
 */
@Composable
fun AboutScreen(
    versionName: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSourceClick: () -> Unit = {},
    onCheckLatest: suspend () -> String? = { null },
) {
    val textResources = LocalResources.current
    var updateState by remember { mutableStateOf(UpdateCheckState.IDLE) }
    val uriHandler = LocalUriHandler.current
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(updateState) {
        if (updateState == UpdateCheckState.CHECKING) {
            try {
                val latest = onCheckLatest()
                latestVersion = latest
                updateState = when {
                    latest == null -> {
                        updateMessage = textResources.getString(R.string.ui_no_release_available)
                        UpdateCheckState.IDLE
                    }
                    AppUpdateRepository.isNewer(latest, versionName) -> UpdateCheckState.AVAILABLE
                    else -> UpdateCheckState.UP_TO_DATE
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                Timber.e(error, "Update check failed")
                updateState = UpdateCheckState.IDLE
                updateMessage = error.message ?: textResources.getString(R.string.ui_could_not_check_updates_please_try_again)
            }
        }
    }

    if (updateMessage != null) BaseAlertDialog(
        onDismissRequest = { updateMessage = null }, text = { Text(updateMessage.orEmpty()) },
        onConfirm = { updateMessage = null },
        showCancel = false,
        confirmLabel = textResources.getString(R.string.ui_ok),
    )
    SettingsSubpageScaffold(
        title = textResources.getString(R.string.ui_about),
        onBack = onDismissRequest,
        modifier = modifier,
    ) {
        AboutContent(
            versionName = versionName,
            updateState = updateState,
            latestVersion = latestVersion,
            onOpenSourceClick = onOpenSourceClick,
            onLicenseClick = { uriHandler.openUri("https://github.com/turingzy/ledger?tab=MIT-1-ov-file") },
            onSourceClick = { uriHandler.openUri("https://github.com/turingzy/ledger") },
            onCheckUpdate = {
                if (updateState == UpdateCheckState.AVAILABLE) uriHandler.openUri("https://github.com/turingzy/ledger/releases/latest")
                else updateState = UpdateCheckState.CHECKING
            },
        )
    }
}

/**
 * 关于页面的主体内容展示组件。
 *
 * @param versionName 当前安装的应用版本号
 * @param updateState 当前更新检测状态
 * @param onOpenSourceClick 点击进入开源许可列表回调
 * @param onLicenseClick 点击查看开源许可证协议回调
 * @param onSourceClick 点击查看 GitHub 源代码仓库回调
 * @param onCheckUpdate 点击检查更新或跳转下载更新回调
 * @param modifier 修饰符
 * @param latestVersion 检测到的远端最新版本号
 */
@Composable
private fun AboutContent(
    versionName: String,
    updateState: UpdateCheckState,
    onOpenSourceClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onSourceClick: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
    latestVersion: String? = null,
) {
    Column(
        modifier.fillMaxWidth().settingsPageScroll(),
        verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.rowVertical),
    ) {
        AboutHero()
        Column(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.rowVertical),
        ) {
            AboutUpdateRow(
                versionName = versionName,
                state = updateState,
                latestVersion = latestVersion,
                onCheckUpdate = onCheckUpdate,
                modifier = Modifier.weight(1f),
            )
            SettingsActionRow(
                stringResource(R.string.ui_open_source_licenses),
                onOpenSourceClick,
                modifier = Modifier.weight(1f),
                subtitle = stringResource(R.string.ui_open_source_libraries),
            )
        }
        SettingsActionRow(stringResource(R.string.ui_license), onLicenseClick, subtitle = "MIT License", externalLink = true)
        SettingsActionRow(
            stringResource(R.string.ui_source_code_on_github),
            onSourceClick,
            subtitle = stringResource(R.string.ui_view_app_source_code),
            externalLink = true,
        )
        Text(
            stringResource(R.string.ui_about_credit) + "\n" + stringResource(R.string.ui_about_copyright),
            Modifier.fillMaxWidth().padding(top = LedgerStyle.spacing.medium),
            style = LedgerStyle.typography.bodySmall,
            color = LedgerStyle.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Centers the project artwork and tagline above the settings actions. */
@Composable
private fun AboutHero() {
    val colors = LedgerStyle.colors
    Column(
        Modifier.fillMaxWidth().padding(bottom = LedgerStyle.spacing.page),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SurfaceBase(
            modifier = Modifier.size(100.dp),
            shape = RoundedCornerShape(25.dp),
            color = colors.surface,
            shadowElevation = 8.dp,
        ) {
            Box {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_background),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                )
                // Adaptive icon layers include a 25% inset on each side of the visible viewport.
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = stringResource(R.string.ui_ledger_app_icon),
                    modifier = Modifier.matchParentSize().scale(1.5f),
                )
            }
        }
        Text(
            stringResource(R.string.app_name),
            Modifier.padding(top = LedgerStyle.spacing.large),
            style = LedgerStyle.typography.screenTitle,
            color = colors.textPrimary,
        )
        Text(
            stringResource(R.string.ui_about_tagline),
            Modifier.padding(top = LedgerStyle.spacing.large),
            style = LedgerStyle.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

/** Shows the installed version and displays the release status after a check. */
@Composable
private fun AboutUpdateRow(
    versionName: String,
    state: UpdateCheckState,
    latestVersion: String?,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LedgerStyle.colors
    val status = when (state) {
        UpdateCheckState.UP_TO_DATE -> stringResource(R.string.ui_you_are_up_to_date)
        UpdateCheckState.AVAILABLE -> latestVersion?.let(::aboutVersionLabel)
        UpdateCheckState.IDLE, UpdateCheckState.CHECKING -> null
    }
    CardBase(modifier, contentPadding = PaddingValues(LedgerStyle.spacing.none)) {
        ListRowBase(
            headline = { Text(stringResource(R.string.ui_about_version_updates), style = MaterialTheme.typography.bodyLarge) },
            supportingText = {
                Text(
                    stringResource(R.string.ui_about_installed_version, aboutVersionLabel(versionName)),
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            supportingSpacing = LedgerStyle.spacing.tiny,
            modifier = Modifier.fillMaxHeight().heightIn(min = LedgerStyle.dimensions.supportingRowMinHeight),
            trailing = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.small),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state == UpdateCheckState.CHECKING) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else if (status != null) {
                        Text(
                            status,
                            Modifier.widthIn(max = 128.dp)
                                .background(colors.fieldBackground, LedgerStyle.shapes.pill)
                                .padding(horizontal = LedgerStyle.spacing.medium, vertical = LedgerStyle.spacing.extraSmall),
                            style = LedgerStyle.typography.label,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(MaterialSymbols.ChevronRight, null, Modifier.size(LedgerStyle.icons.inline), tint = colors.textSecondary)
                }
            },
            enabled = state != UpdateCheckState.CHECKING,
            onClick = onCheckUpdate,
        )
    }
}

/** Pads legacy numeric versions such as 1.0 for display without changing their release comparison. */
private fun aboutVersionLabel(version: String): String {
    val parts = version.removePrefix("v").removePrefix("V").split('.')
    if (parts.any { it.isEmpty() || !it.all(Char::isDigit) }) return version
    return (parts + List((3 - parts.size).coerceAtLeast(0)) { "0" }).joinToString(".")
}

@Preview(showBackground = true)
@Composable
private fun AboutContentPreview() {
    LedgerTheme {
        AboutContent("1.0.0", UpdateCheckState.UP_TO_DATE, onOpenSourceClick = {}, onLicenseClick = {}, onSourceClick = {}, onCheckUpdate = {})
    }
}
