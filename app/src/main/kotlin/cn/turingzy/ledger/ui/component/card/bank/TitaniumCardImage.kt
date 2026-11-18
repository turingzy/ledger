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

internal data class TitaniumCardImage(val bitmap: ImageBitmap, val luminance: Double?)

@Composable
internal fun rememberTitaniumCardImage(uri: Uri?, background: Int): TitaniumCardImage? {
    val resolver = LocalContext.current.contentResolver
    return key(resolver, uri, background) {
        produceState<TitaniumCardImage?>(null) {
            if (uri != null) value = withContext(Dispatchers.IO) { loadCardImage(resolver, uri, background) }
        }.value
    }
}

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
