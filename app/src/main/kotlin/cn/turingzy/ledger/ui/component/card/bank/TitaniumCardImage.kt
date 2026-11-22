package cn.turingzy.ledger.ui.component.card.bank

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.palette.graphics.Palette
import java.io.File
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 钛金属卡片自定义背景图片信息包装类。
 *
 * @property bitmap 裁剪及缩放处理后的卡片位图。
 * @property luminance 卡面可见区域的相对亮度加权平均值（0.0 到 1.0 之间），用于自适应文字前景色；若无法分析则为 `null`。
 */
internal data class TitaniumCardImage(val bitmap: ImageBitmap, val luminance: Double?)

/**
 * 记忆并异步加载银行卡背景图片及亮度分析结果。
 *
 * @param uri 背景图片 URI，可为 content URI、android resource URI 或本地文件路径。
 * @param background 底色 ARGB 值，用于在分析半透明图片时合成有效背景色。
 * @return 加载完成的 [TitaniumCardImage]，加载中或失败时返回 `null`。
 */
@Composable
internal fun rememberTitaniumCardImage(uri: Uri?, background: Int): TitaniumCardImage? {
    val resolver = LocalContext.current.contentResolver
    return key(resolver, uri, background) {
        produceState<TitaniumCardImage?>(null) {
            if (uri != null) value = withContext(Dispatchers.IO) { loadCardImage(resolver, uri, background) }
        }.value
    }
}

/**
 * 从给定的 [Uri] 解码并缩放卡片图片，并分析其相对亮度。
 *
 * @param resolver [ContentResolver] 实例。
 * @param uri 待解码的图片 URI。
 * @param background 背景填充色 ARGB 值。
 * @return 构造的 [TitaniumCardImage]，若读取失败或无权限则返回 `null`。
 */
private fun loadCardImage(resolver: ContentResolver, uri: Uri, background: Int): TitaniumCardImage? = try {
    val source = when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT, ContentResolver.SCHEME_ANDROID_RESOURCE -> ImageDecoder.createSource(resolver, uri)
        ContentResolver.SCHEME_FILE -> uri.path?.let { ImageDecoder.createSource(File(it)) }
        else -> null
    }
    source?.let {
        val bitmap = ImageDecoder.decodeBitmap(it) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longestEdge = maxOf(info.size.width, info.size.height)
            if (longestEdge > 1536) {
                val scale = 1536f / longestEdge
                decoder.setTargetSize(
                    (info.size.width * scale).toInt().coerceAtLeast(1),
                    (info.size.height * scale).toInt().coerceAtLeast(1),
                )
            }
        }
        TitaniumCardImage(bitmap.asImageBitmap(), bitmap.cardLuminance(background))
    }
} catch (error: IOException) {
    timber.log.Timber.w(error, "Unable to decode account card image")
    null
} catch (error: SecurityException) {
    timber.log.Timber.w(error, "Unable to access account card image")
    null
}

/**
 * 分析位图按卡片比例裁剪后的可见区域相对亮度。
 *
 * 采用低分辨率重采样，并使用 [Palette] 色板按像素占比加权计算平均相对亮度。
 *
 * @param background 背景填充色 ARGB 值。
 * @return 相对亮度（0.0 ~ 1.0），若无有效像素则返回 `null`。
 */
private fun Bitmap.cardLuminance(background: Int): Double? {
    // Analyze only the visible crop, including the card color behind transparent pixels.
    val cropWidth = minOf(width, (height * BankCardAspectRatio).toInt().coerceAtLeast(1))
    val cropHeight = minOf(height, (width / BankCardAspectRatio).toInt().coerceAtLeast(1))
    val left = (width - cropWidth) / 2
    val top = (height - cropHeight) / 2
    val sampleWidth = 160
    val sampleHeight = (sampleWidth / BankCardAspectRatio).toInt()
    val sample = createBitmap(sampleWidth, sampleHeight)
    val palette = try {
        Canvas(sample).apply {
            drawColor(background)
            drawBitmap(
                this@cardLuminance, Rect(left, top, left + cropWidth, top + cropHeight),
                RectF(0f, 0f, sampleWidth.toFloat(), sampleHeight.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG),
            )
        }
        Palette.from(sample).clearFilters().generate()
    } finally {
        sample.recycle()
    }
    val population = palette.swatches.sumOf { it.population }
    return if (population > 0) {
        palette.swatches.sumOf { ColorUtils.calculateLuminance(it.rgb) * it.population } / population
    } else null
}
