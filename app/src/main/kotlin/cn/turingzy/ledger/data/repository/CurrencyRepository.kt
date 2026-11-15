package cn.turingzy.ledger.data.repository

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

data class CurrencySelection(val currency: String = "CNY")

@Singleton
class CurrencyRepository @Inject constructor(preferences: AppPreferencesRepository) {
    val selection = preferences.preferences.map { CurrencySelection(it.primaryCurrency) }.distinctUntilChanged()
}
