package cn.turingzy.ledger.ui.component.card.summary

import android.graphics.Bitmap
import android.net.Uri
import cn.turingzy.ledger.data.repository.settings.LedgerCardAppearanceRepository
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import cn.turingzy.ledger.ui.feature.wallet.WalletCardViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SummaryCardViewModelTest {
    private val repository = mockk<LedgerCardAppearanceRepository>(relaxed = true)
    private val ledger = MutableStateFlow(LedgerCardAppearance(transparencyPercent = 25))
    private val wallet = MutableStateFlow(LedgerCardAppearance(transparencyPercent = 75))
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { repository.appearance() } returns ledger
        every { repository.appearance(SummaryCardTarget.WALLET) } returns wallet
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun walletAndLedgerRestoreTheirOwnAppearance() = runTest {
        val ledgerViewModel = SummaryCardViewModel(repository, SummaryCardTarget.LEDGER)
        val walletViewModel = WalletCardViewModel(repository)
        runCurrent()
        assertEquals(25, ledgerViewModel.uiState.value.appearance.transparencyPercent)
        assertEquals(75, walletViewModel.uiState.value.appearance.transparencyPercent)
        wallet.value = wallet.value.copy(transparencyPercent = 90)
        runCurrent()
        assertEquals(90, walletViewModel.uiState.value.appearance.transparencyPercent)
        assertEquals(25, ledgerViewModel.uiState.value.appearance.transparencyPercent)
    }

    @Test
    fun walletTransparencyChangesOnlyWriteWalletPreferences() = runTest {
        val viewModel = WalletCardViewModel(repository)
        runCurrent()
        viewModel.setTransparencyPercent(80)
        runCurrent()
        coVerify(exactly = 1) { repository.setTransparencyPercent(80, SummaryCardTarget.WALLET) }
        coVerify(exactly = 0) { repository.setTransparencyPercent(any(), SummaryCardTarget.LEDGER) }
        assertEquals(25, ledger.value.transparencyPercent)
    }

    @Test
    fun selectedImageIsAppliedDirectlyToWallet() = runTest {
        val uri = mockk<Uri>()
        coEvery { repository.prepareBackground(uri, SummaryCardTarget.WALLET) } returns "unique.jpg"
        val viewModel = WalletCardViewModel(repository)
        runCurrent()
        viewModel.importBackground(uri)
        runCurrent()
        coVerify(exactly = 1) { repository.applyPreparedBackground("unique.jpg", SummaryCardTarget.WALLET) }
        coVerify(exactly = 0) { repository.applyPreparedBackground(any(), SummaryCardTarget.LEDGER) }
        coVerify { repository.discardPreparedBackground("unique.jpg") }
    }
}
