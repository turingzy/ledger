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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import timber.log.Timber

internal enum class RateRefreshStatus { IDLE, REFRESHING, SUCCESS }

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
    private val _rateRefreshStatus = MutableStateFlow(RateRefreshStatus.IDLE)
    internal val rateRefreshStatus = _rateRefreshStatus.asStateFlow()
    private var clearRateRefreshStatus: Job? = null

    private val manualRateEdits = mutableMapOf<Pair<String, String>, Job>()

    fun editManualRate(from: String, to: String, value: String, onSaved: () -> Unit) {
        val currencies = from to to
        manualRateEdits.remove(currencies)?.cancel()
        val rate = value.toBigDecimalOrNull() ?: return
        if (rate.signum() <= 0 || rate.scale() !in -18..18 || rate.precision() > 36) return
        manualRateEdits[currencies] = viewModelScope.launch {
            delay(300)
            try {
                rates.setManualRate(from, to, value)
                onSaved()
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                Timber.e(error, "Unable to save manual rate")
                _message.value = error.message ?: "操作失败，请重试"
            } finally {
                if (manualRateEdits[currencies] == coroutineContext[Job]) manualRateEdits.remove(currencies)
            }
        }
    }

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
    fun save(value: AppPreferences) = perform { preferencesRepository.save(value) }
    suspend fun currentRate(from: String, to: String) = rates.quote(from, to, LocalDate.now())
    fun refreshRates(from: String? = null) = perform {
        clearRateRefreshStatus?.cancel()
        _rateRefreshStatus.value = RateRefreshStatus.REFRESHING
        try {
            val settings = preferencesRepository.preferences.first()
            val currencies = accountsRepository.getAccountsStream().first().map { it.account.currency }.toSet() +
                setOfNotNull(from, settings.primaryCurrency)
            if (from == null && currencies.size < 2) return@perform
            rates.prepare(setOf(LocalDate.now()), currencies, refresh = true)
            _rateRefreshStatus.value = RateRefreshStatus.SUCCESS
            clearRateRefreshStatus = viewModelScope.launch {
                delay(5_000)
                _rateRefreshStatus.value = RateRefreshStatus.IDLE
            }
        } finally {
            if (_rateRefreshStatus.value == RateRefreshStatus.REFRESHING) {
                _rateRefreshStatus.value = RateRefreshStatus.IDLE
            }
        }
    }
}
