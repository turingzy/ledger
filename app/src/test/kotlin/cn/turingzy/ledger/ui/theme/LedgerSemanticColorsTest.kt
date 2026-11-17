package cn.turingzy.ledger.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import cn.turingzy.ledger.model.transaction.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerSemanticColorsTest {
    @Test
    fun transactionColorsFollowTheSuppliedThemeWithoutLosingTheirMeaning() {
        val schemes = listOf(
            lightColorScheme(primary = IosBlue, error = IosRed, tertiary = IosGreen),
            darkColorScheme(primary = IosTeal, error = IosOrange, tertiary = IosPurple),
        )
        schemes.forEach { scheme ->
            val colors = ledgerSemanticColors(scheme)
            assertEquals(scheme.error, colors.transaction(TransactionType.EXPENSE))
            assertEquals(scheme.tertiary, colors.transaction(TransactionType.INCOME))
            assertEquals(IosYellow, colors.transaction(TransactionType.TRANSFER))
            assertEquals(scheme.scrim, colors.onTransaction(TransactionType.TRANSFER))
            assertEquals(colors.expense, colors.expenseChange(increased = true))
            assertEquals(colors.income, colors.expenseChange(increased = false))
        }
    }

    @Test
    fun chartInfoBackgroundKeepsLightAndDarkSurfaceSeparation() {
        val light = lightColorScheme(surface = Color.White, surfaceVariant = SurfaceVariantLight)
        val dark = darkColorScheme(surface = SurfaceDark, surfaceVariant = SurfaceVariantDark)
        val lightInfo = ledgerSemanticColors(light).chartInfoBackground
        assertTrue(lightInfo.luminance() < light.surface.luminance())
        assertTrue(lightInfo.luminance() > light.surfaceVariant.luminance())
        assertEquals(dark.surfaceVariant, ledgerSemanticColors(dark).chartInfoBackground)
    }
}
