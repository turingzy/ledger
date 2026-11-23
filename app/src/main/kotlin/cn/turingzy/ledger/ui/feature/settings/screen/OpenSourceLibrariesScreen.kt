package cn.turingzy.ledger.ui.feature.settings.screen

import cn.turingzy.ledger.ui.feature.settings.component.settingsPageContentPadding
import cn.turingzy.ledger.ui.feature.settings.component.SettingsSubpageScaffold
import cn.turingzy.ledger.ui.interaction.uiClickable
import cn.turingzy.ledger.ui.theme.LedgerStyle
import cn.turingzy.ledger.ui.component.base.SurfaceBase
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.MaterialSymbols

/**
 * 开源库条目数据模型。
 *
 * @property name 开源类库或框架名称
 * @property url 项目官方网站或代码仓库 URL
 */
private data class OpenSourceLibrary(val name: String, val url: String)

/** 应用所依赖的核心开源库与组件列表。 */
private val openSourceLibraries = listOf(
    OpenSourceLibrary("Kotlin", "https://github.com/JetBrains/kotlin"),
    OpenSourceLibrary("Jetpack Compose", "https://developer.android.com/jetpack/androidx/releases/compose"),
    OpenSourceLibrary("Material 3", "https://developer.android.com/jetpack/androidx/releases/compose-material3"),
    OpenSourceLibrary("AndroidX Core", "https://developer.android.com/jetpack/androidx/releases/core"),
    OpenSourceLibrary("AndroidX Activity", "https://developer.android.com/jetpack/androidx/releases/activity"),
    OpenSourceLibrary("AndroidX Lifecycle", "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
    OpenSourceLibrary("Navigation Compose", "https://developer.android.com/jetpack/androidx/releases/navigation"),
    OpenSourceLibrary("Dagger / Hilt", "https://github.com/google/dagger"),
    OpenSourceLibrary("Room", "https://developer.android.com/jetpack/androidx/releases/room"),
    OpenSourceLibrary("DataStore", "https://developer.android.com/jetpack/androidx/releases/datastore"),
    OpenSourceLibrary("kotlinx.coroutines", "https://github.com/Kotlin/kotlinx.coroutines"),
    OpenSourceLibrary("kotlinx.serialization", "https://github.com/Kotlin/kotlinx.serialization"),
    OpenSourceLibrary("Timber", "https://github.com/JakeWharton/timber"),
    OpenSourceLibrary("Material Symbols", "https://github.com/google/material-design-icons"),
)

/**
 * 开源软件许可证与类库展示页面屏幕组件。
 *
 * 罗列应用采用的所有关键开源项目及许可协议类型，点击每个条目可跳转至对应的官方代码仓库或发布主页。
 *
 * @param onBack 返回上一级页面的回调
 * @param modifier 修饰符
 */
@Composable
internal fun OpenSourceLibrariesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val uriHandler = LocalUriHandler.current
    SettingsSubpageScaffold(
        title = textResources.getString(cn.turingzy.ledger.R.string.ui_open_source_licenses),
        onBack = onBack,
        modifier = modifier,
    ) {
        LazyColumn(
            contentPadding = settingsPageContentPadding(),
            verticalArrangement = Arrangement.spacedBy(LedgerStyle.spacing.medium),
        ) {
            items(openSourceLibraries, key = { it.name }) { library ->
                SurfaceBase(shape = LedgerStyle.shapes.card, color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier.fillMaxWidth().uiClickable { uriHandler.openUri(library.url) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(library.name, style = MaterialTheme.typography.bodyLarge)
                            Icon(MaterialSymbols.NorthEast, null, Modifier.size(LedgerStyle.icons.externalLink), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Apache License 2.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
