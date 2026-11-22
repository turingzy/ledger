package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.data.repository.settings.LedgerCardAppearanceRepository
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import cn.turingzy.ledger.ui.component.card.summary.SummaryCardViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 钱包页面汇总卡片外观设置 ViewModel。
 *
 * 继承自 [SummaryCardViewModel]，针对 [SummaryCardTarget.WALLET] 目标配置外观属性。
 */
@HiltViewModel
internal class WalletCardViewModel @Inject constructor(
    repository: LedgerCardAppearanceRepository,
) : SummaryCardViewModel(repository, SummaryCardTarget.WALLET)
