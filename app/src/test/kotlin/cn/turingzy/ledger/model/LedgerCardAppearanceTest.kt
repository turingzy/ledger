package cn.turingzy.ledger.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerCardAppearanceTest {
    @Test
    fun transparencyEndpointsAndMidpointMapToOverlayOpacity() {
        assertEquals(1f, LedgerCardAppearance(transparencyPercent = 0).overlayOpacity, 0f)
        assertEquals(0.5f, LedgerCardAppearance(transparencyPercent = 50).overlayOpacity, 0f)
        assertEquals(0f, LedgerCardAppearance(transparencyPercent = 100).overlayOpacity, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun negativeTransparencyIsRejected() {
        LedgerCardAppearance(transparencyPercent = -1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun transparencyOverOneHundredIsRejected() {
        LedgerCardAppearance(transparencyPercent = 101)
    }
}
