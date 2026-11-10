package cn.turingzy.ledger.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.turingzy.ledger.model.AnalyticsCard
import cn.turingzy.ledger.model.AnalyticsCardSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber

private val Context.analyticsCardsStore by preferencesDataStore(name = "analytics_cards")

@Singleton
class DefaultAnalyticsCardSettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AnalyticsCardSettingsRepository {
    private val orderKey = stringPreferencesKey("order")
    private val disabledKey = stringSetPreferencesKey("disabled")

    override val settings = context.analyticsCardsStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.e(error, "Unable to read analytics card settings")
            emit(emptyPreferences())
        }
        .map(::readSettings)

    override suspend fun setEnabled(card: AnalyticsCard, enabled: Boolean) {
        context.analyticsCardsStore.edit { preferences ->
            val settings = readSettings(preferences).withEnabled(card, enabled)
            preferences[disabledKey] = settings.disabled.map { it.storageKey }.toSet()
        }
    }

    override suspend fun setOrder(order: List<AnalyticsCard>) {
        context.analyticsCardsStore.edit { preferences ->
            val settings = readSettings(preferences).withOrder(order)
            preferences[orderKey] = settings.order.joinToString(",") { it.storageKey }
        }
    }

    private fun readSettings(preferences: Preferences) = AnalyticsCardSettings.fromStored(
        order = preferences[orderKey]?.split(",").orEmpty(),
        disabled = preferences[disabledKey].orEmpty(),
    )
}
