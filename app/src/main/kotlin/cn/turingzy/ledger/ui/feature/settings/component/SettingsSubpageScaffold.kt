package cn.turingzy.ledger.ui.feature.settings.component

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import cn.turingzy.ledger.ui.component.base.BasePageScaffold
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

internal val SettingsPageHeaderHeight = 56.dp
internal val SettingsPageContentTopPadding = SettingsPageHeaderHeight + 12.dp

internal val SettingsPageBottomPadding = 24.dp

@Composable
internal fun Modifier.settingsPageScroll(): Modifier =
    verticalScroll(rememberScrollState()).padding(top = SettingsPageContentTopPadding, bottom = SettingsPageBottomPadding)

@Composable
internal fun SettingsSubpageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = { Box(Modifier.size(LedgerStyle.dimensions.touchTarget)) },
    content: @Composable ColumnScope.() -> Unit,
) {
    var offset by remember(title) { mutableFloatStateOf(0f) }
    val connection = remember(title) {
        object : NestedScrollConnection {
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                offset = (offset - consumed.y).coerceAtLeast(0f)
                return Offset.Zero
            }
        }
    }
    val scrolled by remember(title) { androidx.compose.runtime.derivedStateOf { offset > 0f } }
    val opacity by animateFloatAsState(
        if (scrolled) LedgerStyle.opacity.topBarScrolled else 1f,
        label = "SettingsSubpageHeaderOpacity",
    )
    BasePageScaffold(
        title, modifier, onBack = onBack,
        titleStyle = LedgerStyle.typography.detailPageTitle,
        titlePadding = LedgerStyle.spacing.none,
        backButtonSize = LedgerStyle.dimensions.backButton,
        headerOpacity = opacity,
        actions = actions,
    ) {
        Column(
            Modifier.fillMaxSize().nestedScroll(connection)
                .padding(horizontal = LedgerStyle.spacing.pageHorizontal).testTag("settingsPageContent"),
            content = content,
        )
    }
}
