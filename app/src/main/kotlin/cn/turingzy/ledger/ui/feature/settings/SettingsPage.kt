package cn.turingzy.ledger.ui.feature.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import cn.turingzy.ledger.ui.component.frostedBackdrop
import cn.turingzy.ledger.ui.component.recordBackdrop
import cn.turingzy.ledger.ui.component.rememberBackdropState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.ui.component.TopBarBackButton

@Composable
internal fun SettingsPage(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleLarge,
    avoidKeyboard: Boolean = false,
    action: @Composable () -> Unit = { Box(Modifier.size(48.dp)) },
    content: @Composable ColumnScope.() -> Unit,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val back = onBack ?: onDismissRequest
    BackHandler(onBack = back)
    val backdrop = rememberBackdropState()
    var scrollOffset by remember(title) { mutableFloatStateOf(0f) }
    val scrollConnection = remember(title) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                scrollOffset = (scrollOffset - consumed.y).coerceAtLeast(0f)
                return Offset.Zero
            }
        }
    }
    val headerOpacity by animateFloatAsState(
        targetValue = if (scrollOffset > 0f) 0.55f else 1f,
        label = "SettingsHeaderOpacity",
    )
    Surface(modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize().statusBarsPadding()) {
            Column(
                Modifier.fillMaxSize()
                    .then(if (avoidKeyboard) Modifier.imePadding() else Modifier)
                    .nestedScroll(scrollConnection)
                    .recordBackdrop(backdrop)
                    .padding(horizontal = 20.dp)
                    .testTag("settingsPageContent"),
                content = content,
            )
            Row(
                Modifier.fillMaxWidth().height(SettingsPageHeaderHeight)
                    .frostedBackdrop(
                        backdrop,
                        MaterialTheme.colorScheme.background.copy(alpha = headerOpacity),
                        blurRadius = 3.dp,
                        fadeBottom = 4.dp,
                    )
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TopBarBackButton(
                    contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_back),
                    onClick = back,
                )
                Text(
                    title,
                    Modifier.weight(1f),
                    style = titleStyle,
                    fontWeight = FontWeight.Bold,
                )
                action()
            }
        }
    }
}

internal val SettingsPageHeaderHeight = 56.dp
internal val SettingsPageContentTopPadding = SettingsPageHeaderHeight + 12.dp

internal val SettingsPageBottomPadding = 24.dp

@Composable
internal fun Modifier.settingsPageScroll(): Modifier =
    verticalScroll(rememberScrollState()).padding(top = SettingsPageContentTopPadding, bottom = SettingsPageBottomPadding)
