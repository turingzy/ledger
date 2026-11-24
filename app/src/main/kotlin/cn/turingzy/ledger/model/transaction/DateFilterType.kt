package cn.turingzy.ledger.model.transaction

/**
 * 交易日期过滤范围类型枚举。
 *
 * 用于账单列表、统计分析等模块的时间周期筛选。
 */
enum class DateFilterType {
    /**
     * 按周筛选（周日至周六）。
     */
    WEEK,

    /**
     * 按月筛选（自然月）。
     */
    MONTH,

    /**
     * 按年筛选（自然年）。
     */
    YEAR,

    /**
     * 自定义日期区间范围筛选。
     */
    RANGE,
}
