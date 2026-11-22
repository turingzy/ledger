package cn.turingzy.ledger.ui.component.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * 悬浮底栏的标准高度。
 */
internal val BottomBarHeight = 52.dp

/**
 * 悬浮底栏上下的垂直内边距。
 */
internal val BottomBarVerticalPadding = 12.dp

/**
 * 页面内容为避开悬浮底栏所需的总底部间距（底栏高度加上下外边距）。
 */
internal val BottomBarContentPadding = BottomBarHeight + BottomBarVerticalPadding * 2

/**
 * 提供给下层页面读取的底部导航栏内边距 [androidx.compose.runtime.CompositionLocal]。
 *
 * 为浮动底栏预留可滚动空间，避免页面内容被悬浮底栏遮挡，同时不压缩页面整体可视区域。
 */
internal val LocalBottomBarContentPadding = staticCompositionLocalOf { 0.dp }
