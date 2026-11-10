package cn.turingzy.ledger.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.turingzy.ledger.model.LedgerCardAppearance
import cn.turingzy.ledger.model.SummaryCardTarget
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
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
    override fun appearance(target: SummaryCardTarget) = context.ledgerCardStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.e(error, "Unable to read card preferences")
            emit(emptyPreferences())
        }
        .map { preferences ->
            val keys = keys(target)
            LedgerCardAppearance(
                backgroundFile = preferences[keys.background],
                transparencyPercent = if (preferences[keys.enabled] == false) 0 else (preferences[keys.percent] ?: 50).coerceIn(0, 100),
            )
        }

    override suspend fun setTransparencyPercent(percent: Int, target: SummaryCardTarget) {
        require(percent in 0..100)
        context.ledgerCardStore.edit {
            it[keys(target).percent] = percent
            it.remove(keys(target).enabled)
        }
    }

    override suspend fun resetBackground(target: SummaryCardTarget): Unit = withContext(ioDispatcher) {
        var previous: String? = null
        context.ledgerCardStore.edit {
            previous = it[keys(target).background]
            it.remove(keys(target).background)
        }
        removeBackgroundFile(previous)
    }

    override suspend fun prepareBackground(uri: Uri, target: SummaryCardTarget): String = withContext(ioDispatcher) {
        val destination = createPreparedBackgroundFile(target)
        var saved = false
        try {
            val bitmap = decode(ImageDecoder.createSource(context.contentResolver, uri))
            try {
                destination.outputStream().use { output ->
                    check(bitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)) { "Unable to encode background" }
                }
            } finally {
                bitmap.recycle()
            }
            saved = true
            destination.name
        } finally {
            if (!saved) removeFile(destination)
        }
    }

    private fun createPreparedBackgroundFile(target: SummaryCardTarget): File {
        val prefix = when (target) {
            SummaryCardTarget.LEDGER -> "ledger"
            SummaryCardTarget.WALLET -> "wallet"
        }
        var timestamp = System.currentTimeMillis()
        while (true) {
            val candidate = File(context.cacheDir, "$prefix-$timestamp.jpg")
            if (candidate.createNewFile()) {
                if (!File(context.filesDir, candidate.name).exists()) return candidate
                removeFile(candidate)
            }
            timestamp++
        }
    }

    override suspend fun discardPreparedBackground(filename: String): Unit = withContext(ioDispatcher) {
        removeFile(File(context.cacheDir, filename))
    }

    override suspend fun applyPreparedBackground(filename: String, target: SummaryCardTarget): Unit = withContext(ioDispatcher) {
        val prepared = File(context.cacheDir, filename)
        val destination = File(context.filesDir, filename)
        var saved = false
        try {
            prepared.copyTo(destination, overwrite = true)
            var previous: String? = null
            context.ledgerCardStore.edit {
                previous = it[keys(target).background]
                it[keys(target).background] = destination.name
            }
            saved = true
            removeBackgroundFile(previous)
            removeFile(prepared)
        } finally {
            if (!saved) removeFile(destination)
        }
    }

    private fun removeFile(file: File) {
        if (file.exists() && !file.delete()) Timber.w("Unable to remove card background: %s", file.name)
    }

    private fun removeBackgroundFile(filename: String?) {
        if (filename == null) return
        val file = File(context.filesDir, filename)
        if (file.exists() && !file.delete()) Timber.w("Unable to remove previous ledger background")
    }

    override suspend fun loadBackground(filename: String): Bitmap = withContext(ioDispatcher) {
        decode(ImageDecoder.createSource(File(context.filesDir, filename)))
    }

    private fun keys(target: SummaryCardTarget): AppearanceKeys {
        // Keep the original ledger keys so existing backgrounds and transparency settings survive.
        val prefix = if (target == SummaryCardTarget.LEDGER) "" else "wallet_"
        return AppearanceKeys(
            background = stringPreferencesKey("${prefix}background"),
            enabled = booleanPreferencesKey("${prefix}transparency_enabled"),
            percent = intPreferencesKey("${prefix}transparency_percent"),
        )
    }

    private data class AppearanceKeys(
        val background: Preferences.Key<String>,
        val enabled: Preferences.Key<Boolean>,
        val percent: Preferences.Key<Int>,
    )

    private fun decode(source: ImageDecoder.Source): Bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val longestEdge = maxOf(info.size.width, info.size.height)
        if (longestEdge > 1600) {
            val scale = 1600f / longestEdge
            decoder.setTargetSize((info.size.width * scale).toInt().coerceAtLeast(1), (info.size.height * scale).toInt().coerceAtLeast(1))
        }
    }
}
