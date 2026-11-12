package cn.turingzy.ledger.ui.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.MaterialSymbols

private data class OpenSourceLibrary(val name: String, val url: String)

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
    OpenSourceLibrary("Vico", "https://github.com/patrykandpatrick/vico"),
    OpenSourceLibrary("Material Symbols", "https://github.com/google/material-design-icons"),
)

@Composable
internal fun OpenSourceLibrariesScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    SettingsPage(
        title = "开源许可证",
        onDismissRequest = onBack,
        modifier = modifier,
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = SettingsPageBottomPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(openSourceLibraries, key = { it.name }) { library ->
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier.fillMaxWidth().clickable(role = Role.Button) { uriHandler.openUri(library.url) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(library.name, style = MaterialTheme.typography.bodyLarge)
                            Icon(MaterialSymbols.NorthEast, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("Apache License 2.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
