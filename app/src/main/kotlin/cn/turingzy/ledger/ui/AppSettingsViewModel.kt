package cn.turingzy.ledger.ui

import cn.turingzy.ledger.data.repository.account.AccountRepository
import cn.turingzy.ledger.data.repository.settings.AppPreferencesRepository
import cn.turingzy.ledger.data.repository.currency.ExchangeRateRepository
import cn.turingzy.ledger.model.settings.AppPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import timber.log.Timber

@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val accountsRepository: AccountRepository,
    private val rates: ExchangeRateRepository,
) : ViewModel() {
    val preferences = preferencesRepository.preferences.map<AppPreferences, AppPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val accounts = accountsRepository.getAccountsStream().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun dismissMessage() { _message.value = null }
    private fun perform(action: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        _message.value = null
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { Timber.e(error, "Unable to apply general settings"); _message.value = error.message ?: "操作失败，请重试" }
            finally { _busy.value = false }
        }
    }
    fun save(
        value: AppPreferences,
        rateFrom: String? = null,
        rateValue: String? = null,
        onSaved: () -> Unit = {},
    ) = perform {
        if (rateFrom != null && rateValue != null) rates.setManualRate(rateFrom, value.primaryCurrency, rateValue)
        preferencesRepository.save(value)
        onSaved()
    }
    suspend fun currentRate(from: String, to: String) = rates.quote(from, to, LocalDate.now())
    fun refreshRates() = perform {
        val settings = preferencesRepository.preferences.first()
        val currencies = accountsRepository.getAccountsStream().first().map { it.account.currency }.toSet() + settings.primaryCurrency
        val foreign = currencies - settings.primaryCurrency
        if (foreign.isNotEmpty()) rates.prepare(setOf(LocalDate.now()), currencies, refresh = true)
        val quotes = foreign.map { rates.quote(it, settings.primaryCurrency, LocalDate.now()) }
        _message.value = quotes.minOfOrNull { it.date }?.let { "汇率日期：$it" } ?: "当前无需换算"
    }
}
