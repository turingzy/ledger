package cn.turingzy.ledger.ui.feature.analytics

import java.math.BigDecimal
import java.math.RoundingMode

internal fun analyticsRatio(amount: Long, total: Long): Float {
    if (total <= 0L) return 0f
    return BigDecimal.valueOf(amount)
        .divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP).toFloat()
}
