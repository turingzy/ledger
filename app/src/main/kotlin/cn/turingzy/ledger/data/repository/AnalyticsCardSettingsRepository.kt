package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.model.AnalyticsCard
import cn.turingzy.ledger.model.AnalyticsCardSettings
import kotlinx.coroutines.flow.Flow

interface AnalyticsCardSettingsRepository {
    val settings: Flow<AnalyticsCardSettings>
    suspend fun setEnabled(card: AnalyticsCard, enabled: Boolean)
    suspend fun setOrder(order: List<AnalyticsCard>)
}
