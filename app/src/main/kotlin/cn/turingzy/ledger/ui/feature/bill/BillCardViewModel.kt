package cn.turingzy.ledger.ui.feature.bill

import cn.turingzy.ledger.data.repository.settings.LedgerCardAppearanceRepository
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import cn.turingzy.ledger.ui.component.card.summary.SummaryCardViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class BillCardViewModel @Inject constructor(
    repository: LedgerCardAppearanceRepository,
) : SummaryCardViewModel(repository, SummaryCardTarget.LEDGER)
