package cn.turingzy.ledger.ui.component

import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ScrollTitleContainer(
    title: String,
    scrollOffset: () -> Int,
    onScrollToTop: suspend () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val backdrop = rememberBackdropState()
    val density = LocalDensity.current
    val surface = MaterialTheme.colorScheme.background
    Box(modifier.fillMaxSize()) {
        content(Modifier.recordBackdrop(backdrop))
        val offset = with(density) { scrollOffset().toDp().value }
        val progress = ((offset - 20f) / 36f).coerceIn(0f, 1f)
        if (progress > 0f) {
            Box(
                Modifier.fillMaxWidth().height(44.dp).clipToBounds()
                    .graphicsLayer {
                        alpha = progress
                    }
                    .frostedBackdrop(backdrop, surface.copy(alpha = 0.55f), 3.dp, fadeBottom = 6.dp),
            )
            Row(
                Modifier.fillMaxWidth().height(44.dp).padding(horizontal = LedgerStyle.spacing.extraLarge)
                    .graphicsLayer { alpha = progress },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title,
                    modifier = Modifier.weight(1f).semantics { heading() },
                    fontSize = (22f - 4f * progress).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TopBarIconButton(
                    imageVector = MaterialSymbols.ArrowUpward,
                    contentDescription = stringResource(cn.turingzy.ledger.R.string.ui_scroll_to_top),
                    onClick = { scope.launch { onScrollToTop() } },
                )
            }
        }
    }
}
