package cn.turingzy.ledger.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// Material 3 baseline typography
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
)

object AppTypography {
    val screenTitle = textStyle(28.sp, 34.sp, FontWeight.Bold)
    val detailPageTitle = textStyle(18.sp, 24.sp, FontWeight.Bold)
    val sectionTitle = textStyle(17.sp, 22.sp, FontWeight.SemiBold)
    val cardTitle = textStyle(16.sp, 20.sp)
    val body = textStyle(16.sp, 24.sp).copy(letterSpacing = 0.5.sp)
    val settingsRowTitle = body.copy(fontSize = 17.sp)
    val bodyMedium = textStyle(14.sp, 20.sp)
    val button = bodyMedium.copy(fontWeight = FontWeight.Medium)
    val periodPill = body.copy(fontSize = sectionTitle.fontSize, fontWeight = FontWeight.Medium)
    val calendarMonthLabel = textStyle(11.sp, 13.sp)
    val calendarMonthAmount = numericStyle(15.sp, 17.sp, FontWeight.SemiBold)
    val bodySmall = textStyle(13.sp, 18.sp)
    val label = textStyle(12.sp, 16.sp)
    val caption = textStyle(11.sp, 14.sp)
    val chartLabel = textStyle(10.sp, 12.sp)
    val navigationLabel = caption.copy(fontWeight = FontWeight.Medium)

    val amount = numericStyle(16.sp, 20.sp)
    val transactionTitle = textStyle(15.sp, 18.sp).copy(letterSpacing = (-0.2).sp)
    val transactionAmount = numericStyle(16.sp, 18.sp).copy(letterSpacing = (-0.3).sp)
    val amountSmall = numericStyle(14.sp, 18.sp)
    val amountEmphasis = numericStyle(18.sp, 24.sp, FontWeight.SemiBold)
    val heroAmount = numericStyle(26.sp, 32.sp, FontWeight.Bold).copy(textAlign = TextAlign.Start)
    val searchInput = textStyle(15.sp, 22.sp)
    val inputAmount = numericStyle(22.sp, 28.sp)
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

typealias LedgerTypography = AppTypography

