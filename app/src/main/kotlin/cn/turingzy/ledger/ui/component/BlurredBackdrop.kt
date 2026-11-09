package cn.turingzy.ledger.ui.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberBlurredBackdrop(@DrawableRes resourceId: Int): ImageBitmap? {
    val resources = LocalContext.current.resources
    val backdrop by produceState<ImageBitmap?>(null, resources, resourceId) {
        // 位图解码与卷积属于 CPU 工作，避免阻塞页面首帧和导航动画。
        value = withContext(Dispatchers.Default) {
            val original = BitmapFactory.decodeResource(resources, resourceId)
                ?: return@withContext null
            try {
                val scaled = Bitmap.createScaledBitmap(original, 240, 120, true)
                try {
                    fastBlurBitmap(scaled, radius = 4).asImageBitmap()
                } finally {
                    if (scaled !== original) scaled.recycle()
                }
            } finally {
                original.recycle()
            }
        }
    }
    return backdrop
}

private fun fastBlurBitmap(source: Bitmap, radius: Int): Bitmap {
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val outPixels = IntArray(width * height)

    for (y in 0 until height) {
        val yOffset = y * width
        for (x in 0 until width) {
            var r = 0; var g = 0; var b = 0; var count = 0
            val start = maxOf(0, x - radius)
            val end = minOf(width - 1, x + radius)
            for (i in start..end) {
                val p = pixels[yOffset + i]
                r += (p shr 16) and 0xFF
                g += (p shr 8) and 0xFF
                b += p and 0xFF
                count++
            }
            outPixels[yOffset + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }
    }

    for (x in 0 until width) {
        for (y in 0 until height) {
            var r = 0; var g = 0; var b = 0; var count = 0
            val start = maxOf(0, y - radius)
            val end = minOf(height - 1, y + radius)
            for (i in start..end) {
                val p = outPixels[i * width + x]
                r += (p shr 16) and 0xFF
                g += (p shr 8) and 0xFF
                b += p and 0xFF
                count++
            }
            pixels[y * width + x] = (0xFF shl 24) or ((r / count) shl 16) or ((g / count) shl 8) or (b / count)
        }
    }

    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
}

