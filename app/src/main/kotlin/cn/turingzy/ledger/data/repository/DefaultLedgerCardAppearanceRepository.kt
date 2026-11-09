package cn.turingzy.ledger.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.turingzy.ledger.model.LedgerCardAppearance
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

private val Context.ledgerCardStore by preferencesDataStore(name = "ledger_card")

@Singleton
class DefaultLedgerCardAppearanceRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LedgerCardAppearanceRepository {
    private val backgroundKey = stringPreferencesKey("background")
    private val enabledKey = booleanPreferencesKey("transparency_enabled")
    private val percentKey = intPreferencesKey("transparency_percent")

    override val appearance = context.ledgerCardStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.e(error, "Unable to read ledger card preferences")
            emit(emptyPreferences())
        }
        .map { preferences ->
            LedgerCardAppearance(
                backgroundFile = preferences[backgroundKey],
                transparencyEnabled = preferences[enabledKey] ?: true,
                transparencyPercent = (preferences[percentKey] ?: 50).coerceIn(0, 100),
            )
        }

    override suspend fun setTransparencyEnabled(enabled: Boolean) {
        context.ledgerCardStore.edit { it[enabledKey] = enabled }
    }

    override suspend fun setTransparencyPercent(percent: Int) {
        require(percent in 0..100)
        context.ledgerCardStore.edit { it[percentKey] = percent }
    }

    override suspend fun resetBackground(): Unit = withContext(ioDispatcher) {
        var previous: String? = null
        context.ledgerCardStore.edit {
            previous = it[backgroundKey]
            it.remove(backgroundKey)
        }
        removeBackgroundFile(previous)
    }

    override suspend fun importBackgroundUrl(url: String): Unit = withContext(ioDispatcher) {
        val download = File.createTempFile("ledger-background-", ".tmp", context.cacheDir)
        try {
            CardBackgroundDownloader().download(url, download)
            saveBackground(decode(ImageDecoder.createSource(download)))
        } finally {
            if (download.exists() && !download.delete()) Timber.w("Unable to remove downloaded background")
        }
    }

    override suspend fun importBackground(uri: Uri): Unit = withContext(ioDispatcher) {
        saveBackground(decode(ImageDecoder.createSource(context.contentResolver, uri)))
    }

    private suspend fun saveBackground(bitmap: Bitmap) {
        val target = File(context.filesDir, "ledger-card-${UUID.randomUUID()}.jpg")
        var saved = false
        try {
            target.outputStream().use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) { "Unable to encode background" }
            }
            var previous: String? = null
            context.ledgerCardStore.edit {
                previous = it[backgroundKey]
                it[backgroundKey] = target.name
            }
            saved = true
            removeBackgroundFile(previous)
        } finally {
            bitmap.recycle()
            if (!saved && target.exists() && !target.delete()) Timber.w("Unable to remove incomplete ledger background")
        }
    }

    private fun removeBackgroundFile(filename: String?) {
        if (filename == null) return
        val file = File(context.filesDir, filename)
        if (file.exists() && !file.delete()) Timber.w("Unable to remove previous ledger background")
    }

    override suspend fun loadBackground(filename: String): Bitmap = withContext(ioDispatcher) {
        decode(ImageDecoder.createSource(File(context.filesDir, filename)))
    }

    private fun decode(source: ImageDecoder.Source): Bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val longestEdge = maxOf(info.size.width, info.size.height)
        if (longestEdge > 1600) {
            val scale = 1600f / longestEdge
            decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
        }
    }
}
