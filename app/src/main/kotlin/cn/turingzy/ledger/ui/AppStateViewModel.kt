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

/**
 * 汇率刷新状态枚举。
 *
 * 表示当前汇率同步任务的生命周期阶段，用于在界面上展示刷新进度或成功反馈。
 */
internal enum class RateRefreshStatus {
    /** 空闲状态，当前无正在进行的汇率刷新任务 */
    IDLE,
    /** 正在刷新汇率中 */
    REFRESHING,
    /** 汇率刷新成功，展示成功反馈 */
    SUCCESS,
}

/**
 * 全局应用状态 ViewModel。
 *
 * 负责管理应用级别的全局状态，包括用户偏好设置（主题、语言、本位币等）、
 * 账户列表流、汇率自动/手动刷新及编辑、全局异步繁忙状态和错误消息提示。
 *
 * @param preferencesRepository 用户偏好设置数据仓库。
 * @param accountsRepository 账户数据仓库。
 * @param rates 汇率数据仓库。
 */
@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository,
    private val accountsRepository: AccountRepository,
    private val rates: ExchangeRateRepository,
) : ViewModel() {
    /**
     * 用户偏好设置只读状态流。未加载完成前为 `null`。
     */
    val preferences = preferencesRepository.preferences.map<AppPreferences, AppPreferences?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 账户列表只读状态流，按订阅热度共享。
     */
    val accounts = accountsRepository.getAccountsStream().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _busy = MutableStateFlow(false)
    private var operationInProgress = false

    /**
     * 通用设置控件的繁忙状态。主题切换保存期间保持原有文字颜色。
     */
    val busy = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)

    /**
     * 全局提示消息流，常用于展示操作失败的错误文案。
     */
    val message = _message.asStateFlow()

    private val _rateRefreshStatus = MutableStateFlow(RateRefreshStatus.IDLE)

    /**
     * 汇率刷新状态流，供界面展示加载中或成功提示。
     */
    internal val rateRefreshStatus = _rateRefreshStatus.asStateFlow()
    private var clearRateRefreshStatus: Job? = null

    private val manualRateEdits = mutableMapOf<Pair<String, String>, Job>()

    /**
     * 编辑或设置指定币种对的手动汇率，内置 300ms 防抖机制。
     *
     * @param from 源币种代码（如 "USD"）。
     * @param to 目标币种代码（如 "CNY"）。
     * @param value 汇率数值字符串。
     * @param onSaved 保存成功后的回调函数。
     */
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

    /**
     * 清除当前的全局提示消息。
     */
    fun dismissMessage() { _message.value = null }

    /**
     * 执行互斥的异步挂起任务，自动管理繁忙状态与统一异常捕获。
     *
     * @param action 待执行的挂起操作。
     */
    private fun perform(showBusy: Boolean = true, action: suspend () -> Unit) {
        if (operationInProgress) return
        operationInProgress = true
        _busy.value = showBusy
        _message.value = null
        viewModelScope.launch {
            try { action() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { Timber.e(error, "Unable to apply general settings"); _message.value = error.message ?: "操作失败，请重试" }
            finally {
                operationInProgress = false
                _busy.value = false
            }
        }
    }

    /**
     * 持久化保存用户偏好设置。
     *
     * @param value 最新的偏好设置实体。
     */
    fun save(value: AppPreferences) {
        val current = preferences.value
        val themeOnly = current != null && current.theme != value.theme && current.copy(theme = value.theme) == value
        perform(showBusy = !themeOnly) { preferencesRepository.save(value) }
    }

    /**
     * 查询指定币种对在当天的最新报价汇率。
     *
     * @param from 源币种代码。
     * @param to 目标币种代码。
     * @return 对应的汇率数值报价。
     */
    suspend fun currentRate(from: String, to: String) = rates.quote(from, to, LocalDate.now())

    /**
     * 触发汇率刷新。
     *
     * 会聚合所有账户使用的币种以及本位币，请求汇率仓库拉取当天的最新汇率数据。
     * 刷新成功后会将状态置为 [RateRefreshStatus.SUCCESS] 并保持 5 秒后恢复空闲。
     *
     * @param from 可选的触发源币种，若提供则一并纳入准备列表。
     */
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
