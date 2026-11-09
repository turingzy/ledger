package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.data.repository.LedgerCardAppearanceRepository
import cn.turingzy.ledger.model.SummaryCardTarget
import cn.turingzy.ledger.ui.component.SummaryCardViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class LedgerCardViewModel @Inject constructor(
    repository: LedgerCardAppearanceRepository,
) : SummaryCardViewModel(repository, SummaryCardTarget.LEDGER)
