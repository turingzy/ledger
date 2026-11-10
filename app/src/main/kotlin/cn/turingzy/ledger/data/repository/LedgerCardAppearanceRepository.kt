package cn.turingzy.ledger.data.repository

import android.graphics.Bitmap
import android.net.Uri
import cn.turingzy.ledger.model.LedgerCardAppearance
import cn.turingzy.ledger.model.SummaryCardTarget
import kotlinx.coroutines.flow.Flow

interface LedgerCardAppearanceRepository {
    fun appearance(target: SummaryCardTarget = SummaryCardTarget.LEDGER): Flow<LedgerCardAppearance>
    suspend fun setTransparencyPercent(percent: Int, target: SummaryCardTarget = SummaryCardTarget.LEDGER)
    suspend fun resetBackground(target: SummaryCardTarget = SummaryCardTarget.LEDGER)
    suspend fun prepareBackground(uri: Uri, target: SummaryCardTarget = SummaryCardTarget.LEDGER): String
    suspend fun discardPreparedBackground(filename: String)
    suspend fun applyPreparedBackground(filename: String, target: SummaryCardTarget = SummaryCardTarget.LEDGER)
    suspend fun loadBackground(filename: String): Bitmap
}
