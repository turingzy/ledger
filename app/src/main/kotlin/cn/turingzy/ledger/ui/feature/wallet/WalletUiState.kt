package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.AccountWithTemplate

/**
 * 钱包页面 UI 状态密封接口。
 */
sealed interface WalletUiState {
    /**
     * 正在加载数据状态。
     */
    data object Loading : WalletUiState

    /**
     * 钱包数据加载成功状态。
     *
     * @property totalAsset 计入总资产的折算总额（单位：分）
     * @property totalDebt 负债折算总额（单位：分）
     * @property accounts 账户及关联模板列表
     * @property currencyTotals 各币种资产收支统计列表
     * @property templates 可用的银行卡卡面模板列表
     * @property amountsAvailable 汇率及金额是否有效可用（若缺失汇率可能为 false）
     */
    data class Success(
        val totalAsset: Long,
        val totalDebt: Long,
        val accounts: List<AccountWithTemplate>,
        val currencyTotals: List<WalletCurrencyTotals> = emptyList(),
        val templates: List<CardTemplate> = emptyList(),
        val amountsAvailable: Boolean = true,
    ) : WalletUiState
}
