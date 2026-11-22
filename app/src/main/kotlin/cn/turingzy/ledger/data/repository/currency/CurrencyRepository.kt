package cn.turingzy.ledger.data.repository.currency

import cn.turingzy.ledger.data.repository.settings.AppPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 当前所选货币包装模型。
 *
 * @property currency 货币代码（ISO 4217 格式，如 "CNY"、"USD"），默认为 "CNY"
 */
data class CurrencySelection(val currency: String = "CNY")

/**
 * 货币偏好数据仓库。
 *
 * 监听用户偏好设置中的主币种变动，并向下游提供去重后的主币种流。
 *
 * @param preferences 用户偏好设置仓库
 */
@Singleton
class CurrencyRepository @Inject constructor(preferences: AppPreferencesRepository) {
    /**
     * 当前主币种选择的响应式流，已执行 [distinctUntilChanged] 去重。
     */
    val selection = preferences.preferences.map { CurrencySelection(it.primaryCurrency) }.distinctUntilChanged()
}
