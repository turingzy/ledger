package cn.turingzy.ledger.ui.feature.settings.component

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import cn.turingzy.ledger.ui.component.base.PageScaffoldBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

/** 设置子页面顶部标题栏的标准高度。 */
internal val SettingsPageHeaderHeight = 56.dp

/** 设置子页面内容区域的顶部间距（标题栏高度加上适当留白）。 */
internal val SettingsPageContentTopPadding = SettingsPageHeaderHeight + 12.dp

/** 设置子页面内容区域的底部间距。 */
private val SettingsPageBottomPadding = 24.dp

/**
 * 获取设置子页面的统一内容垂直内边距。
 *
 * 供普通垂直滚动布局及 LazyColumn 等列表组件共享使用，确保内容不被顶部标题栏遮挡。
 *
 * @return 包含顶部和底部间距的 [PaddingValues]
 */
internal fun settingsPageContentPadding(): PaddingValues = PaddingValues(
    top = SettingsPageContentTopPadding,
    bottom = SettingsPageBottomPadding,
)

/**
 * 为设置子页面内容添加垂直滚动能力并应用标准的页面内容内边距。
 */
@Composable
internal fun Modifier.settingsPageScroll(): Modifier =
    verticalScroll(rememberScrollState()).padding(settingsPageContentPadding())

/**
 * 设置子页面的通用脚手架组件。
 *
 * 封装了统一的顶部标题栏、返回/关闭操作按钮、嵌套滚动联动（滚动时标题栏微调透明度）
 * 以及页面标准左右内边距。
 *
 * @param title 页面顶部标题文本
 * @param onBack 点击返回或关闭按钮时的回调
 * @param modifier 修饰符
 * @param closeInsteadOfBack 左侧按钮是否显示为关闭图标而非返回箭头
 * @param actions 顶部标题栏右侧的操作按钮区域
 * @param content 页面主体内容区域
 */
@Composable
internal fun SettingsSubpageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    closeInsteadOfBack: Boolean = false,
    actions: @Composable RowScope.() -> Unit = {},
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
    PageScaffoldBase(
        title, modifier, onBack = onBack,
        headerOpacity = opacity,
        closeInsteadOfBack = closeInsteadOfBack,
        actions = actions,
    ) {
        Column(
            Modifier.fillMaxSize().nestedScroll(connection)
                .padding(horizontal = LedgerStyle.spacing.pageHorizontal).testTag("settingsPageContent"),
            content = content,
        )
    }
}
