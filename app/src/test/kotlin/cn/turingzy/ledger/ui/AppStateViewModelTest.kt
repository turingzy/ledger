package cn.turingzy.ledger.ui

import cn.turingzy.ledger.data.repository.account.AccountRepository
import cn.turingzy.ledger.data.repository.settings.AppPreferencesRepository
import cn.turingzy.ledger.data.repository.currency.ExchangeRateRepository
import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.settings.AppPreferences
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AppStateViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val preferences: AppPreferencesRepository = mockk(relaxed = true) {
        every { this@mockk.preferences } returns flowOf(AppPreferences(primaryCurrency = "CNY"))
    }
    private val accounts: AccountRepository = mockk(relaxed = true) {
        every { getAccountsStream() } returns flowOf(emptyList())
    }
    private val rates: ExchangeRateRepository = mockk(relaxed = true)
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun refreshShowsSuccessForFiveSecondsAndIncludesTheSelectedCurrency() = runTest {
        val model = AppStateViewModel(preferences, accounts, rates)
        coEvery { rates.prepare(any(), any(), refresh = true) } coAnswers { delay(100) }
        val scheduler = dispatcher.scheduler
        model.refreshRates("USD")
        scheduler.runCurrent()
        assertEquals(RateRefreshStatus.REFRESHING, model.rateRefreshStatus.value)
        scheduler.advanceTimeBy(100)
        scheduler.runCurrent()
        assertEquals(RateRefreshStatus.SUCCESS, model.rateRefreshStatus.value)
        coVerify { rates.prepare(any(), setOf("USD", "CNY"), refresh = true) }
        scheduler.advanceTimeBy(4_999)
        assertEquals(RateRefreshStatus.SUCCESS, model.rateRefreshStatus.value)
        scheduler.advanceTimeBy(1)
        scheduler.runCurrent()
        assertEquals(RateRefreshStatus.IDLE, model.rateRefreshStatus.value)
    }

    @Test fun failedRefreshDoesNotShowSuccess() = runTest {
        val model = AppStateViewModel(preferences, accounts, rates)
        coEvery { rates.prepare(any(), any(), refresh = true) } throws java.io.IOException("offline")
        model.refreshRates("USD")
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(RateRefreshStatus.IDLE, model.rateRefreshStatus.value)
        assertEquals("offline", model.message.value)
    }

    @Test fun generalSettingsCanBeSavedWithoutCreatingAnAccount() = runTest {
        val model = AppStateViewModel(preferences, accounts, rates)
        val settings = AppPreferences(language = "en", theme = "dark", primaryCurrency = "EUR")
        model.save(settings)
        model.save(settings)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { preferences.save(settings) }
        coVerify(exactly = 0) { rates.setManualRate(any(), any(), any(), any()) }
        coVerify(exactly = 0) { accounts.createAccount(any()) }
        assertFalse(model.busy.value)
    }

    @Test fun changingPrimaryCurrencyDoesNotChangeAnAccountsOwnCurrency() = runTest {
        every { accounts.getAccountsStream() } returns flowOf(listOf(AccountWithTemplate(Account(name = "港币钱包", type = AccountType.WALLET, currency = "HKD"))))
        val model = AppStateViewModel(preferences, accounts, rates)
        val settings = AppPreferences(primaryCurrency = "JPY")
        model.save(settings)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify { preferences.save(settings) }
        coVerify(exactly = 0) { accounts.updateAccount(any()) }
        coVerify(exactly = 0) { accounts.createAccount(any()) }
    }

    @Test fun failedPreferenceSaveCanBeRetried() = runTest {
        val model = AppStateViewModel(preferences, accounts, rates)
        val settings = AppPreferences(primaryCurrency = "USD")
        coEvery { preferences.save(any()) } throws java.io.IOException("write failure")
        model.save(settings)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("write failure", model.message.value)
        assertFalse(model.busy.value)
        coEvery { preferences.save(any()) } returns Unit
        model.save(settings)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 2) { preferences.save(settings) }
        assertNull(model.message.value)
    }
    @Test fun manualRateEditingSavesOnlyTheLatestValidValueForThePair() = runTest {
        val model = AppStateViewModel(preferences, accounts, rates)
        var saved = false
        model.editManualRate("USD", "CNY", "7") { fail("Superseded value was saved") }
        model.editManualRate("USD", "CNY", "7.25") { saved = true }
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 1) { rates.setManualRate("USD", "CNY", "7.25", any()) }
        coVerify(exactly = 0) { rates.setManualRate("USD", "CNY", "7", any()) }
        coVerify(exactly = 0) { preferences.save(any()) }
        assertTrue(saved)
        assertFalse(model.busy.value)
    }

    @Test fun invalidManualRateDoesNotWriteOrCommitAPendingValue() = runTest {
        val model = AppStateViewModel(preferences, accounts, rates)
        model.editManualRate("USD", "EUR", "7") { fail("Superseded value was saved") }
        model.editManualRate("USD", "EUR", "0") { fail("Invalid value was saved") }
        dispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { rates.setManualRate(any(), any(), any(), any()) }
        coVerify(exactly = 0) { preferences.save(any()) }
    }

}
