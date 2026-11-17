package cn.turingzy.ledger.data.repository.currency

import cn.turingzy.ledger.data.repository.settings.AppPreferencesRepository
import app.cash.turbine.test
import cn.turingzy.ledger.model.settings.AppPreferences
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyRepositoryTest {
    @Test fun everyPageUsesTheSavedPrimaryCurrency() = runTest {
        val values = MutableStateFlow(AppPreferences(primaryCurrency = "CNY"))
        val preferences: AppPreferencesRepository = mockk { every { this@mockk.preferences } returns values }
        val repository = CurrencyRepository(preferences)
        repository.selection.test {
            assertEquals(CurrencySelection("CNY"), awaitItem())
            values.value = values.value.copy(primaryCurrency = "USD")
            assertEquals(CurrencySelection("USD"), awaitItem())
        }
    }
}
