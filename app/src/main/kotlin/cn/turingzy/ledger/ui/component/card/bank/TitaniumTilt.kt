package cn.turingzy.ledger.ui.component.card.bank

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.compose.LifecycleResumeEffect

internal enum class TitaniumDrag { Immediate, AfterLongPress, Disabled }

internal data class TitaniumMotion(val tilt: Offset, val dragStrength: Float)

private val restingMotion = mutableStateOf(TitaniumMotion(Offset.Zero, 0f))

@Composable
internal fun rememberTitaniumTilt(
    enabled: Boolean,
    drag: TitaniumDrag,
): Pair<State<TitaniumMotion>, Modifier> {
    if (!enabled) return restingMotion to Modifier

    val gravity by rememberGravityTilt()
    var touch by remember(drag) { mutableStateOf<Offset?>(null) }
    val dragging = touch != null
    val target = touch ?: gravity
    val followSpring = remember { spring<Float>(dampingRatio = .92f, stiffness = Spring.StiffnessMedium) }
    val returnSpring = remember { spring<Float>(dampingRatio = .84f, stiffness = Spring.StiffnessMediumLow) }
    val tiltSpring = if (dragging) followSpring else returnSpring
    val x = animateFloatAsState(target.x, tiltSpring, label = "Titanium tilt X")
    val y = animateFloatAsState(target.y, tiltSpring, label = "Titanium tilt Y")
    val dragStrength = animateFloatAsState(
        if (dragging) 1f else 0f,
        if (dragging) followSpring else returnSpring,
        label = "Titanium drag highlight",
    )
    val motion = remember(x, y, dragStrength) {
        derivedStateOf {
            TitaniumMotion(Offset(x.value, y.value).limitToUnitCircle(), dragStrength.value.coerceIn(0f, 1f))
        }
    }

    val gesture = if (drag == TitaniumDrag.Disabled) Modifier else Modifier.pointerInput(drag) {
        fun grip() { touch = motion.value.tilt * .45f }

        fun follow(delta: Offset) {
            val movement = Offset(
                delta.x / (size.width * .23f).coerceAtLeast(1f),
                delta.y / (size.height * .29f).coerceAtLeast(1f),
            )
            touch = ((touch ?: Offset.Zero) + movement).limitToUnitCircle()
        }

        fun release() { touch = null }

        try {
            if (drag == TitaniumDrag.AfterLongPress) {
                // Decks reserve ordinary swipes for paging and scrolling.
                detectDragGesturesAfterLongPress(
                    onDragStart = { grip() },
                    onDragEnd = ::release,
                    onDragCancel = ::release,
                    onDrag = { _, delta -> follow(delta) },
                )
            } else {
                detectDragGestures(
                    onDragStart = { grip() },
                    onDragEnd = ::release,
                    onDragCancel = ::release,
                    onDrag = { _, delta -> follow(delta) },
                )
            }
        } finally {
            release()
        }
    }
    return motion to gesture
}

@Composable
private fun rememberGravityTilt(): State<Offset> {
    val view = LocalView.current
    val tilt = remember(view) { mutableStateOf(Offset.Zero) }

    LifecycleResumeEffect(view) {
        val manager = view.context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val screenGravity = when (view.display?.rotation) {
                    Surface.ROTATION_90 -> Offset(-y, -x)
                    Surface.ROTATION_180 -> Offset(-x, y)
                    Surface.ROTATION_270 -> Offset(y, x)
                    else -> Offset(x, -y)
                }
                tilt.value = (screenGravity / SensorManager.GRAVITY_EARTH).limitToUnitCircle()
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }
        if (sensor != null) manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onPauseOrDispose {
            manager?.unregisterListener(listener)
            tilt.value = Offset.Zero
        }
    }
    return tilt
}

private fun Offset.limitToUnitCircle(): Offset = this / getDistance().coerceAtLeast(1f)
