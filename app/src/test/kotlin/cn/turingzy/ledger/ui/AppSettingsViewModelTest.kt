package cn.turingzy.ledger.ui

import cn.turingzy.ledger.data.repository.*
import cn.turingzy.ledger.model.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsViewModelTest {
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

    @Test fun generalSettingsCanBeSavedWithoutCreatingAnAccount() = runTest {
        val model = AppSettingsViewModel(preferences, accounts, rates)
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
        val model = AppSettingsViewModel(preferences, accounts, rates)
        val settings = AppPreferences(primaryCurrency = "JPY")
        model.save(settings)
        dispatcher.scheduler.advanceUntilIdle()
        coVerify { preferences.save(settings) }
        coVerify(exactly = 0) { accounts.updateAccount(any()) }
        coVerify(exactly = 0) { accounts.createAccount(any()) }
    }

    @Test fun failedPreferenceSaveCanBeRetried() = runTest {
        val model = AppSettingsViewModel(preferences, accounts, rates)
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
    @Test fun checkActionSavesEditedRateAndGeneralSettingsTogether() = runTest {
        val model = AppSettingsViewModel(preferences, accounts, rates)
        val settings = AppPreferences(theme = "dark", primaryCurrency = "CNY")
        var saved = false
        model.save(settings, rateFrom = "USD", rateValue = "7.2", onSaved = { saved = true })
        dispatcher.scheduler.advanceUntilIdle()
        coVerifyOrder {
            rates.setManualRate("USD", "CNY", "7.2", any())
            preferences.save(settings)
        }
        assertTrue(saved)
        assertFalse(model.busy.value)
    }

    @Test fun rateFailureKeepsTheDraftUnsavedAndDoesNotApplyGeneralSettings() = runTest {
        val model = AppSettingsViewModel(preferences, accounts, rates)
        val settings = AppPreferences(primaryCurrency = "EUR")
        coEvery { rates.setManualRate(any(), any(), any(), any()) } throws IllegalArgumentException("invalid rate")
        var saved = false
        model.save(settings, rateFrom = "USD", rateValue = "0", onSaved = { saved = true })
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(saved)
        assertEquals("invalid rate", model.message.value)
        coVerify(exactly = 0) { preferences.save(any()) }
    }

}
