package cn.turingzy.ledger.ui.feature.wallet

import kotlin.math.floor
import kotlin.math.roundToInt

internal fun visibleCardPages(count: Int, position: Float, vertical: Boolean): List<Int> {
    if (count <= 0) return emptyList()
    if (count <= 2) return (0 until count).toList()
    if (!vertical) {
        val center = position.roundToInt()
        return (center - 1..center + 1).toList()
    }
    val first = floor(position).toInt()
    return (first until first + minOf(count, 5)).toList()
}

internal fun verticalCardDepth(page: Int, position: Float, count: Int, maxBackCards: Int): Float {
    require(count > 0)
    val start = floor(position).toInt()
    val fraction = position - start
    val from = Math.floorMod(page - start, count).coerceAtMost(maxBackCards)
    val to = Math.floorMod(page - start - 1, count).coerceAtMost(maxBackCards)
    return from + (to - from) * fraction
}
