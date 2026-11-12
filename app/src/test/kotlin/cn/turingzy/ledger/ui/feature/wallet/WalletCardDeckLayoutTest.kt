package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.ui.component.initialCircularCardPage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletCardDeckLayoutTest {
    @Test
    fun visibleLayersNeverDuplicateAnAccountIncludingTwoCardDecks() {
        for (count in 1..20) {
            for (position in listOf(-1f, -0.2f, 0f, 0.5f, 1f, count - 0.2f, count.toFloat())) {
                for (vertical in listOf(false, true)) {
                    val pages = visibleCardPages(count, position, vertical)
                    val accounts = pages.map { Math.floorMod(it, count) }
                    assertEquals(accounts.size, accounts.distinct().size)
                    assertTrue(accounts.size <= 5)
                }
            }
        }
    }

    @Test
    fun settledVerticalCardIsInFrontAndOtherLayersCarryNoFrontSlot() {
        for (count in 1..20) {
            for (selected in 0 until count) {
                val pages = visibleCardPages(count, selected.toFloat(), vertical = true)
                val front = pages.filter { verticalCardDepth(it, selected.toFloat(), count, 3) == 0f }
                assertEquals(listOf(selected), front)
                pages.filter { it != selected }.forEach {
                    assertTrue(verticalCardDepth(it, selected.toFloat(), count, 3) >= 1f)
                }
            }
        }
    }

    @Test
    fun verticalPlacementRemainsContinuousAcrossEveryAnchor() {
        val epsilon = 0.0001f
        for (count in 2..20) {
            for (anchor in -1..count) {
                for (account in 0 until count) {
                    val before = verticalCardDepth(account, anchor - epsilon, count, 3)
                    val after = verticalCardDepth(account, anchor + epsilon, count, 3)
                    assertEquals(before, after, 0.001f)
                }
            }
        }
    }

    @Test
    fun rebasingCircularDeckPreservesEveryLayerPosition() {
        for (count in 2..20) {
            for (account in 0 until count) {
                assertEquals(
                    verticalCardDepth(account, -1f, count, 3),
                    verticalCardDepth(account, (count - 1).toFloat(), count, 3),
                    0f,
                )
                assertEquals(
                    verticalCardDepth(account, count.toFloat(), count, 3),
                    verticalCardDepth(account, 0f, count, 3),
                    0f,
                )
            }
        }
    }

    @Test
    fun circularPagerStartsAwayFromEitherEndAndWrapsInBothDirections() {
        for (count in 2..20) {
            for (selected in 0 until count) {
                val initialPage = initialCircularCardPage(count, selected)
                assertTrue(initialPage > count * 3)
                assertTrue(initialPage < Int.MAX_VALUE - count * 3)
                for (offset in -count * 3..count * 3) {
                    assertEquals(Math.floorMod(selected + offset, count), Math.floorMod(initialPage + offset, count))
                }
            }
        }
        assertEquals(0, initialCircularCardPage(1, 0))
    }

    @Test
    fun twoCardStackKeepsTheExpectedAccountInFrontAcrossBothEnds() {
        for (page in -2..4) {
            val position = page.toFloat()
            val front = visibleCardPages(2, position, vertical = true)
                .minBy { verticalCardDepth(it, position, 2, 3) }
            assertEquals(Math.floorMod(page, 2), Math.floorMod(front, 2))
        }
    }

    @Test
    fun emptyDeckHasNoLayers() {
        assertTrue(visibleCardPages(0, 0f, vertical = false).isEmpty())
        assertTrue(visibleCardPages(0, 0f, vertical = true).isEmpty())
    }
}
