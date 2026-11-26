package cn.turingzy.ledger.data.repository.settings

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
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

/**
 * [LedgerCardAppearanceRepository] 的默认实现。
 *
 * 基于 DataStore Preferences 持久化背景文件名，
 * 并处理应用内部缓存及私有存储目录中的背景图片编码、缩放与文件生命周期。
 *
 * @param context 应用程序上下文
 * @param ioDispatcher 用于执行文件与图片编解码操作的 IO 协程调度器
 */
@Singleton
class DefaultLedgerCardAppearanceRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : LedgerCardAppearanceRepository {
    /**
     * 获取指定卡片的外观设置流。
     *
     * @param target 目标卡片
     */
    override fun appearance(target: SummaryCardTarget) = context.ledgerCardStore.data
        .catch { error ->
            if (error !is IOException) throw error
            Timber.e(error, "Unable to read card preferences")
            emit(emptyPreferences())
        }
        .map { preferences ->
            LedgerCardAppearance(backgroundFile = preferences[backgroundKey(target)])
        }

    /**
     * 重置指定卡片的背景图片，并删除对应的私有存储文件。
     *
     * @param target 目标卡片
     */
    override suspend fun resetBackground(target: SummaryCardTarget): Unit = withContext(ioDispatcher) {
        var previous: String? = null
        context.ledgerCardStore.edit {
            previous = it.remove(backgroundKey(target))
        }
        removeBackgroundFile(previous)
    }

    /**
     * 解码选中的图片、将其等比缩放至最长边不超过 1600 像素后以 JPEG 格式保存至缓存目录。
     *
     * @param uri 原始图片 URI
     * @param target 目标卡片
     * @return 缓存生成的临时文件名
     */
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

    /**
     * 从缓存目录中删除先前准备的临时背景文件。
     *
     * @param filename 临时文件名
     */
    override suspend fun discardPreparedBackground(filename: String): Unit = withContext(ioDispatcher) {
        removeFile(File(context.cacheDir, filename))
    }

    /**
     * 将临时背景文件拷贝至应用私有文件目录并生效，同时更新 DataStore 配置并删除旧背景文件。
     *
     * @param filename 临时文件名
     * @param target 目标卡片
     */
    override suspend fun applyPreparedBackground(filename: String, target: SummaryCardTarget): Unit = withContext(ioDispatcher) {
        val prepared = File(context.cacheDir, filename)
        val destination = File(context.filesDir, filename)
        var saved = false
        try {
            prepared.copyTo(destination, overwrite = true)
            var previous: String? = null
            context.ledgerCardStore.edit {
                val key = backgroundKey(target)
                previous = it[key]
                it[key] = destination.name
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

    /**
     * 从应用私有存储中加载并解码卡片背景位图。
     *
     * @param filename 背景文件名
     * @return 解码后的 Bitmap
     */
    override suspend fun loadBackground(filename: String): Bitmap = withContext(ioDispatcher) {
        decode(ImageDecoder.createSource(File(context.filesDir, filename)))
    }

    /** 沿用账本和钱包各自保存背景图片的原始键。 */
    private fun backgroundKey(target: SummaryCardTarget) = stringPreferencesKey(
        if (target == SummaryCardTarget.LEDGER) "background" else "wallet_background",
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
