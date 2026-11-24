package cn.turingzy.ledger.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

/**
 * 应用全局设计系统样式总入口对象。
 *
 * 聚合颜色、排版、形状、间距、尺寸、透明度、图标规范及交互样式等设计规范令牌。
 */
object AppStyle {
    /** 当前主题下的业务语义颜色 [SemanticColors] */
    val colors: SemanticColors
        @Composable get() {
            val scheme = MaterialTheme.colorScheme
            return remember(scheme) { semanticColors(scheme) }
        }

    /** 应用排版规范 */
    val typography = AppTypography
    /** 应用圆角与形状规范 */
    val shapes = AppShapes
    /** 应用间距规范 */
    val spacing = AppSpacing
    /** 应用尺寸规范 */
    val dimensions = AppDimensions
    /** 应用透明度规范 */
    val opacity = AppOpacity
    /** 应用图标规格规范 */
    val icons = AppIconStyle
    /** 装饰与品牌固定颜色规范 */
    val decorationColors = DecorationColors
}

/** [AppStyle] 的别名 */
typealias LedgerStyle = AppStyle

/**
 * 图标尺寸与字体规范。
 */
object AppIconStyle {
    const val family = "Material Symbols"
    const val variant = "Rounded"
    const val weight = 400
    const val grade = 0
    const val opticalSize = 24

    /** 标准操作图标尺寸 (24dp)。 */
    val default = 24.dp
    /** 列表、行内操作与记账分类图标尺寸 (20dp)。 */
    val inline = 20.dp
    /** 胶囊与状态提示内的紧凑图标尺寸 (16dp)。 */
    val small = 16.dp
    /** 外链图标尺寸 (14dp) */
    val externalLink = 14.dp
    /** 趋势变化微型图标尺寸 (10dp) */
    val trend = 10.dp
    /** 底部导航图标在常态和选中态共用标准画布尺寸。 */
    val navigation = default
    /** 返回导航图标尺寸 (32dp) */
    val back = 32.dp
    /** 图片占位图图标尺寸 (40dp) */
    val imagePlaceholder = 40.dp
}

/** [AppIconStyle] 的别名 */
typealias LedgerIconStyle = AppIconStyle

/**
 * 间距与间隙规范标尺。
 */
object AppSpacing {
    val none = 0.dp
    val hairline = 1.dp
    val tiny = 2.dp
    val extraSmall = 4.dp
    val small = 6.dp
    val medium = 8.dp
    val large = 12.dp
    val extraLarge = 16.dp
    val section = 20.dp
    val block = 24.dp
    val page = 32.dp

    /** 普通页面在系统安全区内侧的水平内容边距。 */
    val pageHorizontal = extraLarge
    val cardPadding = extraLarge
    val rowHorizontal = extraLarge
    val rowVertical = medium
    val inlineGap = medium
    val sectionGap = large
    val labelGap = extraSmall
    val settingsIconGap = 14.dp
}

/** [AppSpacing] 的别名 */
typealias LedgerSpacing = AppSpacing

/**
 * 控件尺寸、行高与边界尺寸规范。
 */
object AppDimensions {
    val formBorderThickness = 1.dp
    val fieldLabelOffset = 10.dp
    val formLabelWidth = 96.dp
    val dialogMaxHeight = 520.dp
    val sheetMaxHeight = 640.dp
    val wideDialogMaxWidth = 560.dp
    /** 主页面、滚动顶栏和子页共用的视觉栏高。 */
    val toolbarHeight = 44.dp
    val supportingRowMinHeight = 58.dp
    val dropdownWidth = 160.dp
    val currencyDropdownWidth = 240.dp
    val dropdownMaxHeight = 200.dp
    val dropdownItemHeight = 36.dp
    val dropdownShadow = 6.dp
    val dropdownBorder = 1.dp
    val listIconContainer = 32.dp
    val settingsDividerInset = AppSpacing.rowHorizontal + listIconContainer + AppSpacing.settingsIconGap
    /** 底部导航与批量操作条的投影高程。 */
    val floatingBarShadow = 2.dp
    /** 主悬浮操作按钮的投影高程。 */
    val floatingActionShadow = 4.dp
    val touchTarget = 48.dp
    val compactIconButton = 36.dp
    val rowMinHeight = 48.dp
    val dividerThickness = 0.5.dp
    val topBarBlur = 3.dp
    val primaryTopBarFade = 6.dp
    val subpageTopBarFade = 4.dp
    val bottomBarBlur = 12.dp
    val compactButtonHeight = 36.dp
    val calendarEmptyIllustrationWidth = 140.dp
    val calendarEmptyIllustrationHeight = 105.dp
}

/** [AppDimensions] 的别名 */
typealias LedgerDimensions = AppDimensions

/**
 * 透明度与遮罩 Alpha 常量规范。
 */
object AppOpacity {
    /** 居中弹窗和模态抽屉共用的黑色遮罩透明度。 */
    const val modalScrim = 0.45f
    const val disabledFieldBorder = 0.12f
    const val subduedChevron = 0.35f
    const val disabled = 0.38f
    const val pressed = 0.05f
    const val toolbarPressed = 0.12f
    const val selectedBackground = 0.08f
    const val cardIconBackground = 0.24f
    const val topBarScrolled = 0.55f
    const val bottomBarTint = 0.72f
}

/** [AppOpacity] 的别名 */
typealias LedgerOpacity = AppOpacity

/**
 * 形状与圆角曲率规范。
 */
object AppShapes {
    val indicator = RoundedCornerShape(3.dp)
    val chartTooltip = RoundedCornerShape(5.dp)
    val control = RoundedCornerShape(8.dp)
    val field = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(16.dp)
    val bankCardRadius = 18.dp
    val bankCard = RoundedCornerShape(bankCardRadius)
    val summaryCard = RoundedCornerShape(20.dp)
    val dialog = RoundedCornerShape(24.dp)
    val confirmationDialog = RoundedCornerShape(28.dp)
    val bottomSheet = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val pill = CircleShape

    val material = Shapes(
        extraSmall = control,
        small = field,
        medium = card,
        large = summaryCard,
        extraLarge = confirmationDialog,
    )
}

/** [AppShapes] 的别名 */
typealias LedgerShapes = AppShapes

/**
 * 交互动画时长与防抖时间参数。
 */
object AppInteractionStyle {
    /** 点击防抖时间间隔（毫秒） */
    const val clickDebounceMillis = 350L
    /** 按下动画淡入时长（毫秒） */
    const val pressEnterMillis = 70
    /** 短按快速轻触动画淡入时长（毫秒） */
    const val quickTapEnterMillis = 40
    /** 短按快速轻触保持时长（毫秒） */
    const val quickTapHoldMillis = 40L
    /** 松开动画淡出时长（毫秒） */
    const val pressExitMillis = 140
    /** 按压取消动画淡出时长（毫秒） */
    const val pressCancelMillis = 100
}

/** [AppInteractionStyle] 的别名 */
typealias LedgerInteractionStyle = AppInteractionStyle
