package cn.turingzy.ledger.ui.feature.analytics

import java.math.BigDecimal
import java.math.RoundingMode

internal data class CategoryStackSegment(
    val categoryId: Long,
    val colorIndex: Int,
    val start: Float,
    val end: Float,
)

internal fun categoryStackSegments(rankings: List<CategoryExpenseRanking>): List<CategoryStackSegment> {
    val total = rankings.fold(BigDecimal.ZERO) { sum, ranking ->
        sum.add(BigDecimal.valueOf(ranking.totalAmount.coerceAtLeast(0L)))
    }
    if (total.signum() == 0) return emptyList()
    var accumulated = BigDecimal.ZERO
    return rankings.mapIndexedNotNull { index, ranking ->
        if (ranking.totalAmount <= 0L) return@mapIndexedNotNull null
        val start = accumulated.divide(total, 12, RoundingMode.HALF_UP).toFloat()
        accumulated = accumulated.add(BigDecimal.valueOf(ranking.totalAmount))
        CategoryStackSegment(
            categoryId = ranking.categoryId,
            colorIndex = index,
            start = start,
            end = accumulated.divide(total, 12, RoundingMode.HALF_UP).toFloat(),
        )
    }
}
