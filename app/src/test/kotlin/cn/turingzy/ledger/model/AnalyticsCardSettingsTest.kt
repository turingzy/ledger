package cn.turingzy.ledger.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsCardSettingsTest {
    @Test
    fun defaultsEnableAllCardsInTheirOriginalOrder() {
        assertEquals(AnalyticsCard.entries.toList(), AnalyticsCardSettings().enabledCards)
    }

    @Test
    fun storedSettingsIgnoreUnknownAndDuplicateEntriesAndAppendMissingCards() {
        val restored = AnalyticsCardSettings.fromStored(
            listOf("categories", "removed", "categories", "trend"),
            setOf("comparison", "removed"),
        )
        assertEquals(listOf(AnalyticsCard.CATEGORIES, AnalyticsCard.TREND, AnalyticsCard.COMPARISON), restored.order)
        assertEquals(listOf(AnalyticsCard.CATEGORIES, AnalyticsCard.TREND), restored.enabledCards)
    }

    @Test
    fun disablingAndReenablingPreservesOrderIncludingWhenEverythingIsDisabled() {
        val reordered = AnalyticsCardSettings().move(AnalyticsCard.CATEGORIES, 0)
        val disabled = AnalyticsCard.entries.fold(reordered) { settings, card -> settings.withEnabled(card, false) }
        assertTrue(disabled.enabledCards.isEmpty())
        val enabled = disabled.withEnabled(AnalyticsCard.CATEGORIES, true)
        assertEquals(reordered.order, enabled.order)
        assertEquals(listOf(AnalyticsCard.CATEGORIES), enabled.enabledCards)
    }

    @Test
    fun cardsMoveInBothDirectionsWithoutDuplicatesOrChangingVisibility() {
        val initial = AnalyticsCardSettings().withEnabled(AnalyticsCard.TREND, false)
        val first = initial.move(AnalyticsCard.CATEGORIES, 0)
        assertEquals(listOf(AnalyticsCard.CATEGORIES, AnalyticsCard.COMPARISON, AnalyticsCard.TREND), first.order)
        assertEquals(initial.disabled, first.disabled)
        assertEquals(initial, first.move(AnalyticsCard.CATEGORIES, 2))
        assertEquals(first, first.move(AnalyticsCard.CATEGORIES, -1))
        assertEquals(first, first.move(AnalyticsCard.CATEGORIES, 3))
    }

    @Test
    fun storedOrderRoundTripKeepsDisabledCardsInTheirChosenPosition() {
        val settings = AnalyticsCardSettings().move(AnalyticsCard.TREND, 0).withEnabled(AnalyticsCard.COMPARISON, false)
        val restored = AnalyticsCardSettings.fromStored(settings.order.map { it.storageKey }, settings.disabled.map { it.storageKey }.toSet())
        assertEquals(settings, restored)
        assertEquals(AnalyticsCard.entries.size, settings.withOrder(listOf(AnalyticsCard.TREND, AnalyticsCard.TREND)).order.size)
    }
}
