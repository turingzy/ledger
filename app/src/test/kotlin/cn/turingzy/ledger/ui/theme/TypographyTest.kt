package cn.turingzy.ledger.ui.theme

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnitType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TypographyTest {
    @Test
    fun amountColumnsUseTabularFiguresAndLogicalEndAlignment() {
        with(AppTypography) {
            listOf(amount, amountSmall, amountEmphasis, inputAmount, transactionAmount).forEach { style ->
                assertEquals("tnum", style.fontFeatureSettings)
                assertEquals(TextAlign.End, style.textAlign)
            }
            assertEquals("tnum", heroAmount.fontFeatureSettings)
            assertEquals(TextAlign.Start, heroAmount.textAlign)
            assertEquals("tnum", cardNumber.fontFeatureSettings)
            assertEquals(TextAlign.Start, cardNumber.textAlign)
        }
    }

    @Test
    fun textRolesScaleWithUserFontSizeAndProvideExplicitLineHeight() {
        with(AppTypography) {
            listOf(
                screenTitle, sectionTitle, cardTitle, body, bodyMedium, bodySmall,
                label, caption, chartLabel, navigationLabel, amount, amountSmall,
                amountEmphasis, heroAmount, inputAmount, cardNumber, transactionTitle, transactionAmount,
                button, periodPill, calendarMonthLabel, calendarMonthAmount,
            ).forEach { style ->
                assertEquals(TextUnitType.Sp, style.fontSize.type)
                assertEquals(TextUnitType.Sp, style.lineHeight.type)
                assertTrue(style.lineHeight.value >= style.fontSize.value)
            }
        }
    }
}
