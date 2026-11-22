package cn.turingzy.ledger.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Material 3 基线排版样式配置 */
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)

/**
 * 应用文本与数值排版规范对象。
 *
 * 预设各种层级的标题、正文、标签以及启用等宽数字（tnum）特性的金额样式。
 */
object AppTypography {
    /** 页面大标题样式 (28sp / 粗体) */
    val screenTitle = textStyle(28.sp, 34.sp, FontWeight.Bold)
    /** 详情页标题样式 (18sp / 粗体) */
    val detailPageTitle = textStyle(18.sp, 24.sp, FontWeight.Bold)
    /** 分区栏目标题样式 (17sp / 半粗) */
    val sectionTitle = textStyle(17.sp, 22.sp, FontWeight.SemiBold)
    /** 卡片标题样式 (16sp) */
    val cardTitle = textStyle(16.sp, 20.sp)
    /** 标准正文样式 (16sp / 0.5sp 字距) */
    val body = textStyle(16.sp, 24.sp).copy(letterSpacing = 0.5.sp)
    /** 设置列表项标题样式 (17sp) */
    val settingsRowTitle = body.copy(fontSize = 17.sp)
    /** 中号正文样式 (14sp) */
    val bodyMedium = textStyle(14.sp, 20.sp)
    /** 按钮文字样式 (14sp / 中粗) */
    val button = bodyMedium.copy(fontWeight = FontWeight.Medium)
    /** 周期胶囊选择器文本样式 (17sp / 中粗) */
    val periodPill = body.copy(fontSize = sectionTitle.fontSize, fontWeight = FontWeight.Medium)
    /** 消费日历月份标签样式 (11sp) */
    val calendarMonthLabel = textStyle(11.sp, 13.sp)
    /** 消费日历月份金额数值样式 (15sp / 等宽 / 半粗) */
    val calendarMonthAmount = numericStyle(15.sp, 17.sp, FontWeight.SemiBold)
    /** 小号正文样式 (13sp) */
    val bodySmall = textStyle(13.sp, 18.sp)
    /** 字段与辅助标签样式 (12sp) */
    val label = textStyle(12.sp, 16.sp)
    /** 辅助说明及脚注样式 (11sp) */
    val caption = textStyle(11.sp, 14.sp)
    /** 图表轴线及图例文本样式 (10sp) */
    val chartLabel = textStyle(10.sp, 12.sp)
    /** 底部导航栏文字样式 (11sp / 中粗) */
    val navigationLabel = caption.copy(fontWeight = FontWeight.Medium)

    /** 标准金额数值样式 (16sp / 等宽 / 右对齐) */
    val amount = numericStyle(16.sp, 20.sp)
    /** 交易明细标题样式 (15sp / 紧凑字距) */
    val transactionTitle = textStyle(15.sp, 18.sp).copy(letterSpacing = (-0.2).sp)
    /** 交易流水金额数值样式 (16sp / 等宽 / 紧凑字距) */
    val transactionAmount = numericStyle(16.sp, 18.sp).copy(letterSpacing = (-0.3).sp)
    /** 次要小号金额数值样式 (14sp / 等宽) */
    val amountSmall = numericStyle(14.sp, 18.sp)
    /** 强调大号金额数值样式 (18sp / 等宽 / 半粗) */
    val amountEmphasis = numericStyle(18.sp, 24.sp, FontWeight.SemiBold)
    /** 页面焦点总资产巨幅金额数值样式 (26sp / 等宽 / 粗体 / 左对齐) */
    val heroAmount = numericStyle(26.sp, 32.sp, FontWeight.Bold).copy(textAlign = TextAlign.Start)
    /** 搜索框输入文本样式 (15sp) */
    val searchInput = textStyle(15.sp, 22.sp)
    /** 记账金额输入大号数值样式 (22sp / 等宽) */
    val inputAmount = numericStyle(22.sp, 28.sp)
    /** 银行卡卡号显示样式 (13sp / 等宽 / 中粗 / 宽松字距) */
    val cardNumber = numericStyle(13.sp, 18.sp, FontWeight.Medium).copy(
        letterSpacing = 1.5.sp,
        textAlign = TextAlign.Start,
    )

    private fun numericStyle(
        fontSize: TextUnit,
        lineHeight: TextUnit,
        fontWeight: FontWeight = FontWeight.Normal,
    ): TextStyle = textStyle(fontSize, lineHeight, fontWeight).copy(
        fontFeatureSettings = "tnum",
        textAlign = TextAlign.End,
    )

    private fun textStyle(
        fontSize: TextUnit,
        lineHeight: TextUnit,
        fontWeight: FontWeight = FontWeight.Normal,
    ): TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = fontWeight,
        fontSize = fontSize,
        lineHeight = lineHeight,
        letterSpacing = 0.sp,
    )
}

/** [AppTypography] 的别名 */
typealias LedgerTypography = AppTypography

