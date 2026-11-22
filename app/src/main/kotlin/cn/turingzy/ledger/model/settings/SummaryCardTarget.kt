package cn.turingzy.ledger.model.settings

/**
 * 汇总概览卡片的目标应用场景枚举。
 *
 * 区分外观设置是应用于首页账本卡片还是钱包资产卡片。
 */
enum class SummaryCardTarget {
    /**
     * 账本主页收支汇总卡片。
     */
    LEDGER,

    /**
     * 钱包页面总资产汇总卡片。
     */
    WALLET,
}
