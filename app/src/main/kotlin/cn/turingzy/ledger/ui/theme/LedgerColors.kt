package cn.turingzy.ledger.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.transaction.TransactionType

@Immutable
data class LedgerSemanticColors(
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
    val currencyUnit: Color,
    val background: Color,
    val surface: Color,
    val fieldBackground: Color,
    val divider: Color,
    val accountDebit: Color,
    val accountCredit: Color,
    val accountWallet: Color,
    val accountCash: Color,
    val chartComparison: Color,
    val chartInfoBackground: Color,
) {
    fun transaction(type: TransactionType): Color = when (type) {
        TransactionType.EXPENSE -> expense
        TransactionType.INCOME -> income
        TransactionType.TRANSFER -> transfer
    }

    fun onTransaction(type: TransactionType): Color = when (type) {
        TransactionType.EXPENSE -> onExpense
        TransactionType.INCOME -> onIncome
        TransactionType.TRANSFER -> onTransfer
    }

    fun account(type: AccountType): Color = when (type) {
        AccountType.DEBIT -> accountDebit
        AccountType.CREDIT -> accountCredit
        AccountType.WALLET -> accountWallet
        AccountType.CASH -> accountCash
    }

    fun expenseChange(increased: Boolean): Color = if (increased) expense else income
}

fun ledgerSemanticColors(scheme: ColorScheme): LedgerSemanticColors = LedgerSemanticColors(
    expense = scheme.error,
    income = scheme.tertiary,
    transfer = IosYellow,
    onExpense = scheme.onPrimary,
    onIncome = scheme.onPrimary,
    onTransfer = scheme.scrim,
    balance = scheme.primary,
    success = scheme.tertiary,
    warning = IosOrange,
    destructive = scheme.error,
    info = scheme.primary,
    textPrimary = scheme.onSurface,
    textSecondary = scheme.onSurfaceVariant,
    currencyUnit = scheme.secondary,
    background = scheme.background,
    surface = scheme.surface,
    fieldBackground = scheme.surfaceVariant,
    divider = scheme.outlineVariant,
    accountDebit = scheme.primary,
    accountCredit = IosPurple,
    accountWallet = scheme.tertiary,
    accountCash = IosOrange,
    chartComparison = scheme.onSurfaceVariant,
    chartInfoBackground = if (scheme.surface.luminance() > 0.5f) {
        lerp(scheme.surface, scheme.surfaceVariant, 0.48f)
    } else {
        scheme.surfaceVariant
    },
)

// Card artwork and brand colors stay fixed when the application theme changes.
object LedgerDecorationColors {
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
    fun colorPickerPalette(scheme: ColorScheme): List<Color> = listOf(
        colorPickerDefault, colorPickerOutline, scheme.primary, scheme.tertiary, scheme.error,
        IosOrange, IosPurple, IosYellow, IosTeal,
    )
}
