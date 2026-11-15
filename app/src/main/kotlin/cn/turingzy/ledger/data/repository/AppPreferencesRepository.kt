package cn.turingzy.ledger.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import cn.turingzy.ledger.model.AppPreferences
import cn.turingzy.ledger.model.defaultPrimaryCurrency
import cn.turingzy.ledger.model.supportedCurrencyCodes
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.appPreferences by preferencesDataStore("app_preferences")

@Singleton
class AppPreferencesRepository @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val language = stringPreferencesKey("language")
    private val theme = stringPreferencesKey("theme")
    private val currency = stringPreferencesKey("primary_currency")
    val preferences = context.appPreferences.data.catch { error ->
        if (error !is IOException) throw error
        Timber.e(error, "Unable to read app preferences")
        emit(emptyPreferences())
    }.map { AppPreferences(it[language] ?: "system", it[theme] ?: "system", it[currency] ?: defaultPrimaryCurrency(context.resources.configuration.locales[0])) }

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
