package cn.turingzy.ledger.ui.component

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

internal val BottomBarHeight = 52.dp
internal val BottomBarVerticalPadding = 12.dp
internal val BottomBarContentPadding = BottomBarHeight + BottomBarVerticalPadding * 2

// Reserve scrollable space for the floating bar without shortening the page viewport.
internal val LocalBottomBarContentPadding = staticCompositionLocalOf { 0.dp }
