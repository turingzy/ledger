package cn.turingzy.ledger.ui.feature.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import kotlinx.coroutines.delay

private enum class UpdatePreviewState { IDLE, CHECKING, AVAILABLE }

@Composable
fun AboutScreen(
    versionName: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSourceClick: () -> Unit = {},
) {
    var updateState by remember { mutableStateOf(UpdatePreviewState.IDLE) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(updateState) {
        if (updateState == UpdatePreviewState.CHECKING) {
            delay(1_200)
            updateState = UpdatePreviewState.AVAILABLE
        }
    }

    SettingsPage(
        title = "关于",
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        titleStyle = MaterialTheme.typography.headlineMedium,
    ) {
        AboutContent(
            versionName = versionName,
            updateState = updateState,
            onOpenSourceClick = onOpenSourceClick,
            onLicenseClick = { uriHandler.openUri("https://github.com/turingzy/ledger?tab=MIT-1-ov-file") },
            onSourceClick = { uriHandler.openUri("https://github.com/turingzy/ledger") },
            onCheckUpdate = { updateState = UpdatePreviewState.CHECKING },
        )
    }
}

@Composable
private fun AboutContent(
    versionName: String,
    updateState: UpdatePreviewState,
    onOpenSourceClick: () -> Unit,
    onLicenseClick: () -> Unit,
    onSourceClick: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Surface(
            onClick = onCheckUpdate,
            enabled = updateState != UpdatePreviewState.CHECKING,
            modifier = Modifier.semantics { contentDescription = "检查更新" },
            shape = RoundedCornerShape(16.dp),
            color = colors.surface,
            border = BorderStroke(0.5.dp, colors.onSurface.copy(alpha = 0.28f)),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(80.dp).clip(CircleShape)) {
                    Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
                    Image(painterResource(R.drawable.ic_launcher_foreground), "Ledger 应用图标", Modifier.fillMaxSize())
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ledger", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            versionName,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant.copy(alpha = 0.65f),
                        )
                        when (updateState) {
                            UpdatePreviewState.CHECKING -> CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 1.5.dp)
                            UpdatePreviewState.AVAILABLE -> Text("1.0.1", style = MaterialTheme.typography.bodySmall, color = colors.primary)
                            UpdatePreviewState.IDLE -> Unit
                        }
                    }
                    Text("TuringZY", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Ledger 是一款本地优先的个人记账与资产管理应用，帮助你记录日常收支、管理账户与资产。数据保存在设备本地，注重隐私，纯粹实用。",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        SettingsActionRow("开源许可证", onOpenSourceClick, subtitle = "应用使用的开源库")
        Spacer(Modifier.height(8.dp))
        SettingsActionRow("开源协议", onLicenseClick, subtitle = "MIT License", externalLink = true)
        Spacer(Modifier.height(8.dp))
        SettingsActionRow("Github 上的源代码", onSourceClick, subtitle = "查看此应用的源代码", externalLink = true)
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutContentPreview() {
    MaterialTheme {
        AboutContent("1.0.0", UpdatePreviewState.AVAILABLE, onOpenSourceClick = {}, onLicenseClick = {}, onSourceClick = {}, onCheckUpdate = {})
    }
}
