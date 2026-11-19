package cn.turingzy.ledger.ui.component.base

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.interaction.UiInteractionKind
import cn.turingzy.ledger.ui.theme.LedgerStyle

@Composable
fun PageScaffoldBase(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    backDescription: String = stringResource(R.string.ui_back),
    scrolled: Boolean = false,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    titleStyle: TextStyle = LedgerStyle.typography.sectionTitle,
    titlePadding: Dp = LedgerStyle.spacing.labelGap,
    backButtonSize: Dp = LedgerStyle.dimensions.touchTarget,
    headerOpacity: Float = if (scrolled) LedgerStyle.opacity.topBarScrolled else 1f,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = LedgerStyle.colors
    val backdrop = rememberBackdropStateBaseBase()
    val density = LocalDensity.current
    var barHeight by remember { mutableStateOf(LedgerStyle.dimensions.toolbarHeight) }
    BackHandler(enabled = onBack != null) { onBack?.invoke() }
    PageFrameBase(modifier, windowInsets) {
        Box(Modifier.fillMaxSize().recordBackdropBase(backdrop)) {
            content(PaddingValues(top = barHeight))
        }
        Box(Modifier.fillMaxWidth().blockTouchesBehind().frostedBackdropBase(
            backdrop,
            colors.background.copy(alpha = headerOpacity),
            blurRadius = LedgerStyle.dimensions.topBarBlur,
            fadeBottom = LedgerStyle.dimensions.topBarFade,
        )) {
            Row(
                Modifier.fillMaxWidth().heightIn(min = LedgerStyle.dimensions.toolbarHeight)
                    .padding(horizontal = LedgerStyle.spacing.medium).testTag("basePageTopBar")
                    .onSizeChanged { barHeight = with(density) { it.height.toDp() } },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBack != null) IconButtonBase(
                    MaterialSymbols.ChevronLeft, backDescription, onBack,
                    modifier = Modifier.size(backButtonSize),
                    iconSize = LedgerStyle.icons.back, interaction = UiInteractionKind.Navigation,
                )
                Text(
                    title, Modifier.weight(1f).padding(horizontal = titlePadding),
                    style = titleStyle,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                actions()
            }
        }
    }
}

@Composable
fun PageFrameBase(
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.safeDrawing,
    content: @Composable BoxScope.() -> Unit,
) {
    SurfaceBase(modifier.fillMaxSize(), color = LedgerStyle.colors.background, contentColor = LedgerStyle.colors.textPrimary) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(windowInsets), content = content)
    }
}
