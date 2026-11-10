package cn.turingzy.ledger.ui.feature.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsCategoryLayoutTest {
    @Test
    fun segmentsFillTheBarWithoutGapsAndPreserveCategoryOrder() {
        val segments = categoryStackSegments(listOf(ranking(1L, 600L), ranking(2L, 300L), ranking(3L, 100L)))
        assertEquals(listOf(1L, 2L, 3L), segments.map { it.categoryId })
        assertEquals(0f, segments.first().start)
        assertEquals(1f, segments.last().end)
        assertEquals(0.6f, segments[0].end, 0.00001f)
        segments.zipWithNext().forEach { (left, right) -> assertEquals(left.end, right.start) }
    }

    @Test
    fun zeroAmountsDoNotCreateSegmentsOrShiftPaletteIndices() {
        val segments = categoryStackSegments(listOf(ranking(1L, 0L), ranking(2L, 100L)))
        assertEquals(2L, segments.single().categoryId)
        assertEquals(1, segments.single().colorIndex)
        assertEquals(0f, segments.single().start)
        assertEquals(1f, segments.single().end)
        assertTrue(categoryStackSegments(emptyList()).isEmpty())
        assertTrue(categoryStackSegments(listOf(ranking(1L, 0L))).isEmpty())
    }

    @Test
    fun largeAmountsAndSmallCategoriesKeepTheirCorrectProportions() {
        val large = categoryStackSegments(listOf(ranking(1L, Long.MAX_VALUE), ranking(2L, Long.MAX_VALUE)))
        assertEquals(0.5f, large[0].end)
        assertEquals(1f, large[1].end)
        val small = categoryStackSegments(listOf(ranking(1L, 9999L), ranking(2L, 1L)))
        assertTrue(small[1].end > small[1].start)
        assertEquals(1f, small.last().end)
    }

    private fun ranking(id: Long, amount: Long) = CategoryExpenseRanking(
        categoryId = id,
        categoryName = "分类$id",
        totalAmount = amount,
        percentage = 0f,
    )
}
