package cn.turingzy.ledger.ui.feature.analytics.calculation

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 计算某项金额在总金额中的占比。
 *
 * @param amount 分项金额（分）。
 * @param total 总金额（分）。
 * @return 占比浮点数（0f ~ 1f），四舍五入保留 6 位小数；若总额小于等于 0 则返回 0f。
 */
internal fun analyticsRatio(amount: Long, total: Long): Float {
    if (total <= 0L) return 0f
    return BigDecimal.valueOf(amount)
        .divide(BigDecimal.valueOf(total), 6, RoundingMode.HALF_UP).toFloat()
}
