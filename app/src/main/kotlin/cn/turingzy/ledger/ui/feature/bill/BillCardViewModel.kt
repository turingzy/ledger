package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.data.repository.settings.LedgerCardAppearanceRepository
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import cn.turingzy.ledger.ui.component.card.summary.SummaryCardViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * 账单页面汇总卡片的外观及偏好配置 ViewModel。
 *
 * 绑定 [SummaryCardTarget.LEDGER] 目标，负责管理账单主页头部卡片的卡面样式与设置。
 */
@HiltViewModel
internal class BillCardViewModel @Inject constructor(
    repository: LedgerCardAppearanceRepository,
) : SummaryCardViewModel(repository, SummaryCardTarget.LEDGER)
