package cn.turingzy.ledger.data.repository.settings

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.model.settings.defaultPrimaryCurrency
import cn.turingzy.ledger.model.settings.supportedCurrencyCodes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.appPreferences by preferencesDataStore("app_preferences")

/**
 * 应用偏好设置数据仓库。
 *
 * 基于 Jetpack DataStore Preferences 管理应用的通用设置，如语言、主题以及主货币。
 *
 * @param context 应用程序上下文
 */
@Singleton
class AppPreferencesRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val language = stringPreferencesKey("language")
    private val theme = stringPreferencesKey("theme")
    private val currency = stringPreferencesKey("primary_currency")

    /**
     * 应用偏好设置的响应式数据流。
     *
     * 包含语言、主题模式以及主币种代码。
     */
    val preferences = context.appPreferences.data.catch { error ->
        if (error !is IOException) throw error
        Timber.e(error, "Unable to read app preferences")
        emit(emptyPreferences())
    }.map { AppPreferences(it[language] ?: "system", it[theme] ?: "system", it[currency] ?: defaultPrimaryCurrency(context.resources.configuration.locales[0])) }

    /**
     * 保存应用偏好设置。
     *
     * @param value 待保存的偏好设置对象
     * @throws IllegalArgumentException 当语言、主题或主币种代码不受支持时抛出
     */
    suspend fun save(value: AppPreferences) {
        require(value.language in setOf("system", "zh", "en") && value.theme in setOf("system", "light", "dark"))
        require(value.primaryCurrency in supportedCurrencyCodes)
        context.appPreferences.edit {
            it[language] = value.language
            it[theme] = value.theme
            it[currency] = value.primaryCurrency
        }
    }
}
