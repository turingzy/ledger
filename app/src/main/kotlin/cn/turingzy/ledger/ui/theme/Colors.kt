package cn.turingzy.ledger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.transaction.TransactionType

/**
 * 应用业务语义颜色集合。
 *
 * 集中管理交易类型、账户类型、图表以及各层级界面元素在当前主题下的语义化配色。
 *
 * @property expense 支出颜色
 * @property income 收入颜色
 * @property transfer 转账颜色
 * @property onExpense 支出前景文本/图标色
 * @property onIncome 收入前景文本/图标色
 * @property onTransfer 转账前景文本/图标色
 * @property balance 结余/常规金额颜色
 * @property success 成功状态色
 * @property warning 警告状态色
 * @property destructive 破坏性操作/危险状态色
 * @property info 提示信息色
 * @property textPrimary 主文本颜色
 * @property textSecondary 次要文本颜色
 * @property textSupporting 辅助说明、时间与占位文本颜色
 * @property background 页面底色
 * @property surface 卡片及面板表面色
 * @property overlay 弹窗、抽屉和下拉菜单的表面色
 * @property overlayFieldBackground 弹层内输入框及选项控件底色
 * @property fieldBackground 输入框等控件背景色
 * @property outline 卡片和控件的常规边框色
 * @property divider 分割线颜色
 * @property accountDebit 储蓄卡账户指示色
 * @property accountCredit 信用卡账户指示色
 * @property accountWallet 电子钱包账户指示色
 * @property accountCash 现金账户指示色
 * @property chartComparison 图表对比基准线颜色
 * @property chartInfoBackground 图表浮层背景色
 */
@Immutable
data class SemanticColors(
    val expense: Color,
    val income: Color,
    val transfer: Color,
    val onExpense: Color,
    val onIncome: Color,
    val onTransfer: Color,
    val balance: Color,
    val success: Color,
    val warning: Color,
    val destructive: Color,
    val info: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textSupporting: Color,
    val background: Color,
    val surface: Color,
    val overlay: Color,
    val overlayFieldBackground: Color,
    val fieldBackground: Color,
    val outline: Color,
    val divider: Color,
    val accountDebit: Color,
    val accountCredit: Color,
    val accountWallet: Color,
    val accountCash: Color,
    val chartComparison: Color,
    val chartInfoBackground: Color,
) {
    /** 币种单位与其他辅助文字使用同一灰阶。 */
    val currencyUnit: Color get() = textSupporting

    /**
     * 根据交易类型获取对应的语义颜色。
     *
     * @param type 交易类型（支出、收入、转账）
     * @return 对应的颜色
     */
    fun transaction(type: TransactionType): Color = when (type) {
        TransactionType.EXPENSE -> expense
        TransactionType.INCOME -> income
        TransactionType.TRANSFER -> transfer
    }

    /**
     * 根据交易类型获取对应的前景色（用于在交易色块上绘制文字或图标）。
     *
     * @param type 交易类型（支出、收入、转账）
     * @return 对应的前景色
     */
    fun onTransaction(type: TransactionType): Color = when (type) {
        TransactionType.EXPENSE -> onExpense
        TransactionType.INCOME -> onIncome
        TransactionType.TRANSFER -> onTransfer
    }

    /**
     * 根据账户类型获取对应的指示颜色。
     *
     * @param type 账户类型
     * @return 对应的指示色
     */
    fun account(type: AccountType): Color = when (type) {
        AccountType.DEBIT -> accountDebit
        AccountType.CREDIT -> accountCredit
        AccountType.WALLET -> accountWallet
        AccountType.CASH -> accountCash
    }

    /**
     * 根据支出环比变化趋势获取指示颜色（支出增加为支出警示色，支出减少为收入色）。
     *
     * @param increased 是否增加
     * @return 对应的趋势颜色
     */
    fun expenseChange(increased: Boolean): Color = if (increased) expense else income
}

/**
 * 根据 Material 3 [ColorScheme] 构造对应的业务语义颜色 [SemanticColors]。
 *
 * @param scheme Material 3 配色方案
 * @return 组装后的业务语义色彩对象
 */
fun semanticColors(scheme: ColorScheme): SemanticColors {
    val lightSurface = scheme.surface.luminance() > 0.5f
    return SemanticColors(
        expense = scheme.error,
        income = scheme.tertiary,
        transfer = IosYellow,
        onExpense = scheme.onPrimary,
        onIncome = scheme.onPrimary,
        onTransfer = scheme.scrim,
        balance = scheme.primary,
        success = if (lightSurface) SuccessLight else SuccessDark,
        warning = if (lightSurface) WarningLight else WarningDark,
        destructive = scheme.error,
        info = scheme.primary,
        textPrimary = scheme.onSurface,
        textSecondary = scheme.onSurfaceVariant,
        textSupporting = if (lightSurface) TextSupportingLight else TextSupportingDark,
        background = scheme.background,
        surface = scheme.surface,
        overlay = if (lightSurface) scheme.surface else OverlayDark,
        overlayFieldBackground = if (lightSurface) scheme.surfaceVariant else OverlayFieldDark,
        fieldBackground = scheme.surfaceVariant,
        outline = scheme.outlineVariant,
        divider = if (lightSurface) DividerLight else DividerDark,
        accountDebit = scheme.primary,
        accountCredit = IosPurple,
        accountWallet = scheme.tertiary,
        accountCash = IosOrange,
        chartComparison = scheme.onSurfaceVariant,
        chartInfoBackground = if (lightSurface) {
            lerp(scheme.surface, scheme.surfaceVariant, 0.48f)
        } else {
            scheme.surfaceVariant
        },
    )
}

/**
 * 装饰与品牌固定颜色集合。
 *
 * 包含银行卡卡面渐变色、品牌标识专属颜色等不受系统深浅色主题切换影响的常量色值。
 */
object DecorationColors {
    val centurion = listOf(Color(0xFF0D0E11), Color(0xFF141519), Color(0xFF08080A))
    val classicBlue = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    val emerald = listOf(Color(0xFF075E37), Color(0xFF03361E))
    val creditPurple = listOf(Color(0xFF2D143D), Color(0xFF1A0D26))
    val walletBlue = listOf(Color(0xFF0A438C), Color(0xFF052148))
    val obsidian = listOf(Color(0xFF1C1C1E), Color(0xFF121214))
    val unionPayRed = Color(0xFFE60012)
    val unionPayBlue = Color(0xFF003882)
    val unionPayGreen = Color(0xFF008453)
    val onBrand = Color.White
    val chip = IosYellow
    val colorPickerOutline = IosLabel
    val colorPickerDefault = SurfaceLight

    /**
     * 获取颜色选择器可用的调色板列表。
     *
     * @param scheme 当前主题配色方案
     * @return 预置颜色值列表
     */
    fun colorPickerPalette(scheme: ColorScheme): List<Color> = listOf(
        colorPickerDefault, colorPickerOutline, scheme.primary, scheme.tertiary, scheme.error,
        IosOrange, IosPurple, IosYellow, IosTeal,
    )
}

/** [SemanticColors] 的别名 */
typealias LedgerSemanticColors = SemanticColors
/** [DecorationColors] 的别名 */
typealias LedgerDecorationColors = DecorationColors
/** [semanticColors] 的别名函数 */
fun ledgerSemanticColors(scheme: ColorScheme): SemanticColors = semanticColors(scheme)

