package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.LocalIndication
import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import kotlinx.coroutines.delay

private enum class UpdatePreviewState { IDLE, CHECKING, AVAILABLE, UPDATING }

@Composable
fun AboutSheet(
    versionName: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var updateState by remember { mutableStateOf(UpdatePreviewState.IDLE) }
    val uriHandler = LocalUriHandler.current

    LaunchedEffect(updateState) {
        if (updateState == UpdatePreviewState.CHECKING) {
            delay(1_200)
            updateState = UpdatePreviewState.AVAILABLE
        }
    }

    SettingsBottomSheet(
        title = "关于",
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        AboutContent(
            versionName = versionName,
            updateState = updateState,
            onProjectClick = { uriHandler.openUri("https://github.com/turingzy/ledger") },
            onCheckUpdate = {
                updateState = when (updateState) {
                    UpdatePreviewState.IDLE -> UpdatePreviewState.CHECKING
                    UpdatePreviewState.AVAILABLE -> UpdatePreviewState.UPDATING
                    else -> updateState
                }
            },
        )
    }
}

@Composable
private fun AboutContent(
    versionName: String,
    updateState: UpdatePreviewState,
    onProjectClick: () -> Unit,
    onCheckUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(80.dp).clip(CircleShape)) {
                Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
                Image(painterResource(R.drawable.ic_launcher_foreground), "Ledger 应用图标", Modifier.fillMaxSize())
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("turingzy", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                        .clickable(role = Role.Button, onClick = onProjectClick).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Ledger", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                    Icon(
                        MaterialSymbols.NorthEast,
                        "打开 Ledger 项目",
                        Modifier.align(Alignment.Top).padding(top = 2.dp).size(14.dp),
                        tint = colors.onSurface,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Ledger 是一款本地优先的个人记账与资产管理应用，帮助你记录日常收支、管理账户与资产。数据保存在设备本地，注重隐私，纯粹实用。",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("开源协议", style = MaterialTheme.typography.bodyLarge)
            Text("MIT License", style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("版本 $versionName", style = MaterialTheme.typography.bodyLarge)
                when (updateState) {
                    UpdatePreviewState.CHECKING -> {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp)
                    }
                    UpdatePreviewState.AVAILABLE, UpdatePreviewState.UPDATING -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("→", style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                            Text("1.0.1", style = MaterialTheme.typography.bodyMedium, color = colors.primary)
                        }
                    }
                    UpdatePreviewState.IDLE -> Unit
                }
            }
            Box(Modifier.size(width = 80.dp, height = 48.dp), contentAlignment = Alignment.CenterEnd) {
                if (updateState == UpdatePreviewState.UPDATING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = colors.tertiary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = LocalIndication.current,
                            enabled = updateState != UpdatePreviewState.CHECKING,
                            role = Role.Button,
                            onClick = onCheckUpdate,
                        ),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            if (updateState == UpdatePreviewState.AVAILABLE) "开始更新" else "检查更新",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (updateState == UpdatePreviewState.CHECKING) colors.onSurface.copy(alpha = 0.38f) else colors.primary,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AboutContentPreview() {
    MaterialTheme {
        AboutContent("1.0.0", UpdatePreviewState.AVAILABLE, onProjectClick = {}, onCheckUpdate = {})
    }
}
