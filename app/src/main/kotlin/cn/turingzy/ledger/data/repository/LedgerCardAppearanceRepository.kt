package cn.turingzy.ledger.data.repository

import android.graphics.Bitmap
import android.net.Uri
import cn.turingzy.ledger.model.LedgerCardAppearance
import cn.turingzy.ledger.model.SummaryCardTarget
import kotlinx.coroutines.flow.Flow

interface LedgerCardAppearanceRepository {
    fun appearance(target: SummaryCardTarget = SummaryCardTarget.LEDGER): Flow<LedgerCardAppearance>
    suspend fun setTransparencyEnabled(enabled: Boolean, target: SummaryCardTarget = SummaryCardTarget.LEDGER)
    suspend fun setTransparencyPercent(percent: Int, target: SummaryCardTarget = SummaryCardTarget.LEDGER)
    suspend fun resetBackground(target: SummaryCardTarget = SummaryCardTarget.LEDGER)
    suspend fun importBackgroundUrl(url: String, target: SummaryCardTarget = SummaryCardTarget.LEDGER)
    suspend fun importBackground(uri: Uri, target: SummaryCardTarget = SummaryCardTarget.LEDGER)
    suspend fun loadBackground(filename: String): Bitmap
}
