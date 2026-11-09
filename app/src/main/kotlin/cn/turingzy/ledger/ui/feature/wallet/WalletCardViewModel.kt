package cn.turingzy.ledger.ui.feature.wallet

import cn.turingzy.ledger.data.repository.LedgerCardAppearanceRepository
import cn.turingzy.ledger.model.SummaryCardTarget
import cn.turingzy.ledger.ui.component.SummaryCardViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class WalletCardViewModel @Inject constructor(
    repository: LedgerCardAppearanceRepository,
) : SummaryCardViewModel(repository, SummaryCardTarget.WALLET)
