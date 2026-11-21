package cn.turingzy.ledger.ui.feature.settings.screen

import cn.turingzy.ledger.ui.feature.settings.component.SettingsActionRow
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.feature.settings.component.settingsPageScroll
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import cn.turingzy.ledger.ui.component.base.BaseAlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import kotlinx.coroutines.CancellationException
import cn.turingzy.ledger.data.repository.settings.AppUpdateRepository
import timber.log.Timber

private enum class UpdateCheckState { IDLE, CHECKING, AVAILABLE }

@Composable
fun AboutScreen(
    versionName: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSourceClick: () -> Unit = {},
    onCheckLatest: suspend () -> String? = { null },
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var updateState by remember { mutableStateOf(UpdateCheckState.IDLE) }
    val uriHandler = LocalUriHandler.current
    var latestVersion by remember { mutableStateOf<String?>(null) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(updateState) {
        if (updateState == UpdateCheckState.CHECKING) {
            try {
                val latest = onCheckLatest()
                latestVersion = latest
                if (latest != null && AppUpdateRepository.isNewer(latest, versionName)) updateState = UpdateCheckState.AVAILABLE
                else {
                    updateState = UpdateCheckState.IDLE
                    updateMessage = if (latest == null) textResources.getString(cn.turingzy.ledger.R.string.ui_no_release_available) else textResources.getString(cn.turingzy.ledger.R.string.ui_you_are_up_to_date)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                Timber.e(error, "Update check failed")
                updateState = UpdateCheckState.IDLE
                updateMessage = error.message ?: textResources.getString(cn.turingzy.ledger.R.string.ui_could_not_check_updates_please_try_again)
            }
        }
    }

    if (updateMessage != null) BaseAlertDialog(
        onDismissRequest = { updateMessage = null }, text = { Text(updateMessage.orEmpty()) },
        onConfirm = { updateMessage = null },
        showCancel = false,
        confirmLabel = textResources.getString(cn.turingzy.ledger.R.string.ui_ok),
    )
    SettingsSubpageScaffold(
        title = textResources.getString(cn.turingzy.ledger.R.string.ui_about),
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
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().settingsPageScroll()) {
        SurfaceBase(
            onClick = onCheckUpdate,
            enabled = updateState != UpdateCheckState.CHECKING,
            modifier = Modifier.semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_check_for_updates) },
            shape = LedgerStyle.shapes.card,
            color = colors.surface,
            border = BorderStroke(0.5.dp, colors.onSurface.copy(alpha = 0.28f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(LedgerStyle.spacing.large),
                horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.section),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(80.dp).clip(CircleShape)) {
                    Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
                    Image(painterResource(R.drawable.ic_launcher_foreground), textResources.getString(cn.turingzy.ledger.R.string.ui_ledger_app_icon), Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.extraSmall)) {
                    Text("Ledger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            versionName,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                        )
                        when (updateState) {
                            UpdateCheckState.CHECKING -> CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 1.5.dp)
                            UpdateCheckState.AVAILABLE -> Text(latestVersion.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.primary)
                            UpdateCheckState.IDLE -> Unit
                        }
                    }
                    Text("TuringZY", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            textResources.getString(cn.turingzy.ledger.R.string.ui_ledger_is_a_local_first_personal_finance_app),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        SettingsActionRow(textResources.getString(cn.turingzy.ledger.R.string.ui_open_source_licenses), onOpenSourceClick, subtitle = textResources.getString(cn.turingzy.ledger.R.string.ui_open_source_libraries))
        Spacer(Modifier.height(8.dp))
        SettingsActionRow(textResources.getString(cn.turingzy.ledger.R.string.ui_license), onLicenseClick, subtitle = "MIT License", externalLink = true)
        Spacer(Modifier.height(8.dp))
        SettingsActionRow(textResources.getString(cn.turingzy.ledger.R.string.ui_source_code_on_github), onSourceClick, subtitle = textResources.getString(cn.turingzy.ledger.R.string.ui_view_app_source_code), externalLink = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutContentPreview() {
    MaterialTheme {
        AboutContent("1.0.0", UpdateCheckState.AVAILABLE, onOpenSourceClick = {}, onLicenseClick = {}, onSourceClick = {}, onCheckUpdate = {})
    }
}
