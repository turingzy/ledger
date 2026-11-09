package cn.turingzy.ledger.data.repository

import android.graphics.Bitmap
import android.net.Uri
import cn.turingzy.ledger.model.LedgerCardAppearance
import kotlinx.coroutines.flow.Flow

interface LedgerCardAppearanceRepository {
    val appearance: Flow<LedgerCardAppearance>
    suspend fun setTransparencyEnabled(enabled: Boolean)
    suspend fun setTransparencyPercent(percent: Int)
    suspend fun resetBackground()
    suspend fun importBackgroundUrl(url: String)
    suspend fun importBackground(uri: Uri)
    suspend fun loadBackground(filename: String): Bitmap
}
