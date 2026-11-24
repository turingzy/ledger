package cn.turingzy.ledger.ui

import androidx.compose.foundation.background
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import cn.turingzy.ledger.ui.component.navigation.BottomBarContentPadding
import cn.turingzy.ledger.ui.component.navigation.LocalBottomBarContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onLayoutRectChanged
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.navigation.LedgerNavHost
import cn.turingzy.ledger.navigation.TopLevelDestination
import cn.turingzy.ledger.ui.component.base.rememberBackdropStateBaseBase
import cn.turingzy.ledger.ui.component.base.recordBackdropBase
import cn.turingzy.ledger.ui.component.navigation.LedgerBottomBar
import cn.turingzy.ledger.ui.component.navigation.LedgerFloatingActionButton
import cn.turingzy.ledger.ui.interaction.LocalSystemClickFeedback
import cn.turingzy.ledger.ui.interaction.rememberSystemClickFeedback

/**
 * 账本应用主框架界面。
 *
 * 托管顶级底部导航栏 [LedgerBottomBar]、全局记账悬浮按钮 [LedgerFloatingActionButton]，
 * 以及主导航宿主 [LedgerNavHost]。管理顶级目的地切换、子页面与多选操作下的底部栏显隐，
 * 并处理全屏任务转场时的遮罩与裁剪联动。
 *
 * @param modifier 应用于根容器的修饰符。
 */
@Composable
fun LedgerApp(
    modifier: Modifier = Modifier,
) {
    val navigationBackdrop = rememberBackdropStateBaseBase()
    val systemClickFeedback = rememberSystemClickFeedback()
    var currentTopLevelDestination by rememberSaveable { mutableStateOf(TopLevelDestination.BILL) }

    var billSelectionVisible by remember { mutableStateOf(false) }
    val isBillTab = currentTopLevelDestination == TopLevelDestination.BILL
    val subpages = remember { mutableStateMapOf<TopLevelDestination, Boolean>() }
    val taskTops = remember { mutableStateMapOf<Pair<TopLevelDestination, String>, Int>() }
    var chromeTop by remember { mutableIntStateOf(0) }
    val isSubpage = subpages[currentTopLevelDestination] == true
    val billSelectionActive = isBillTab && billSelectionVisible
    var showGlobalRecordSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = !isBillTab) {
        currentTopLevelDestination = TopLevelDestination.BILL
    }

    CompositionLocalProvider(
        LocalSystemClickFeedback provides systemClickFeedback,
        LocalBottomBarContentPadding provides if (billSelectionActive) 0.dp else BottomBarContentPadding,
    ) {
        Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            LedgerNavHost(
                currentDestination = currentTopLevelDestination,
                showRecordSheet = showGlobalRecordSheet,
                onRecordSheetDismiss = { showGlobalRecordSheet = false },
                onNavigateToRecord = { showGlobalRecordSheet = true },
                onBillSelectionVisibleChange = { billSelectionVisible = it },
                onSubpageChange = { destination, visible -> subpages[destination] = visible },
                onTaskTopChange = { destination, entryId, top ->
                    val task = destination to entryId
                    if (top == null) taskTops.remove(task) else taskTops[task] = top
                },
                modifier = Modifier.fillMaxSize().recordBackdropBase(navigationBackdrop)
                    .then(if (billSelectionActive) Modifier.navigationBarsPadding() else Modifier),
            )

            Box(
                Modifier.matchParentSize()
                    .onLayoutRectChanged(debounceMillis = 0) { chromeTop = it.boundsInRoot.top }
                    .graphicsLayer {
                        val taskTop = taskTops.entries.minOfOrNull { (task, top) ->
                            if (task.first == currentTopLevelDestination) top else Int.MAX_VALUE
                        }
                        // Keep the bottom controls visually behind the moving task.
                        clip = taskTop != null && taskTop != Int.MAX_VALUE
                        if (clip) shape = RevealedChromeShape((requireNotNull(taskTop) - chromeTop).toFloat())
                    },
            ) {
                LedgerFloatingActionButton(
                    label = stringResource(R.string.ui_transaction),
                    icon = MaterialSymbols.EditSquare,
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    visible = isBillTab && !isSubpage && !billSelectionVisible,
                    onClick = { showGlobalRecordSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = BottomBarContentPadding),
                )

                // Reattach the bar as the root page returns, without a separate fade-in.
                if (!isSubpage) LedgerBottomBar(
                    visible = !billSelectionActive,
                    destinations = TopLevelDestination.entries,
                    currentDestination = currentTopLevelDestination,
                    onNavigateToDestination = { currentTopLevelDestination = it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                    backdrop = navigationBackdrop,
                )
            }
        }
    }
}

/**
 * 动态裁剪形状，用于将底部控制条等界面元素裁剪至全屏任务动态顶边之上。
 *
 * 当全屏任务向上滑动展开时，保证底部栏视觉上保持在其下方并被精准遮挡。
 *
 * @property bottom 裁剪矩形的下边界纵坐标（像素）。
 */
private data class RevealedChromeShape(val bottom: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline =
        Outline.Rectangle(Rect(0f, 0f, size.width, bottom.coerceIn(0f, size.height)))
}
