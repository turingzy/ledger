package cn.turingzy.ledger.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntSize
import kotlin.math.hypot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/** 从页面右上角揭开新主题，旧画面只在切换时截取一帧。 */
@Composable
internal fun ThemeWipe(darkTheme: Boolean, content: @Composable (Boolean, Boolean) -> Unit) {
    val requestedTheme by rememberUpdatedState(darkTheme)
    var displayedTheme by remember { mutableStateOf(darkTheme) }
    var bottomTheme by remember { mutableStateOf(darkTheme) }
    var capture by remember { mutableStateOf<CompletableDeferred<Unit>?>(null) }
    var previousFrame by remember { mutableStateOf<ImageBitmap?>(null) }
    val layer = rememberGraphicsLayer()
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val reveal = remember { Animatable(1f) }
    val revealPath = remember { Path() }

    LaunchedEffect(Unit) {
        snapshotFlow { requestedTheme }.collect { nextTheme ->
            if (nextTheme == displayedTheme) return@collect
            val frameReady = CompletableDeferred<Unit>()
            capture = frameReady
            val snapshot = try {
                withTimeoutOrNull(250) {
                    frameReady.await()
                    layer.toImageBitmap()
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: RuntimeException) {
                Timber.w(error, "Unable to capture the previous theme")
                null
            } finally {
                capture = null
            }

            val target = requestedTheme
            if (target == displayedTheme) return@collect
            if (snapshot != null) {
                reveal.snapTo(0f)
                previousFrame = snapshot
            }
            displayedTheme = target
            if (snapshot != null) reveal.animateTo(1f, tween(durationMillis = 560, easing = LinearEasing))
            bottomTheme = target
            previousFrame = null
            if (frameReady.isCompleted) layer.record(density, layoutDirection, IntSize(1, 1)) {}
        }
    }

    Box(Modifier.fillMaxSize().drawWithContent {
        val request = capture
        if (request != null && !request.isCompleted) {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
            request.complete(Unit)
        } else {
            drawContent()
        }
        previousFrame?.let { frame ->
            val progress = reveal.value
            if (progress < 1f) {
                val radius = hypot(size.width, size.height) * progress
                revealPath.rewind()
                revealPath.addOval(Rect(size.width - radius, -radius, size.width + radius, radius))
                clipPath(revealPath, ClipOp.Difference) { drawImage(frame) }
            }
        }
    }) {
        content(displayedTheme, bottomTheme)
    }
}
