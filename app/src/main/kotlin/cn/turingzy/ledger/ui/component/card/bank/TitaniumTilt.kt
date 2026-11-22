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

/**
 * 钛金属卡片手势拖拽倾斜模式。
 */
internal enum class TitaniumDrag {
    /** 立即响应单指滑动拖拽。 */
    Immediate,

    /** 长按后才响应拖拽倾斜，避免与横向滑动翻页手势冲突。 */
    AfterLongPress,

    /** 禁用手势拖拽倾斜。 */
    Disabled,
}

/**
 * 钛金属物理动效状态。
 *
 * @property tilt 归一化倾斜偏移量，限制在单位圆半径内（-1.0 到 1.0）。
 * @property dragStrength 当前拖拽高光增强系数（0.0 表示静止或纯陀螺仪，1.0 表示手势拖拽中）。
 */
internal data class TitaniumMotion(val tilt: Offset, val dragStrength: Float)

private val restingMotion = mutableStateOf(TitaniumMotion(Offset.Zero, 0f))

/**
 * 记忆并计算钛金属卡片的动态倾斜与光照动效。
 *
 * 结合设备重力感应器（陀螺仪）和用户触摸拖拽手势，通过弹性物理动画（Spring）平滑插值倾斜偏移量与高光强度。
 *
 * @param enabled 是否开启物理倾斜动效。
 * @param drag 触摸拖拽触发策略。
 * @param tuning 物理调节参数。
 * @return 包含动画状态 [TitaniumMotion] 和手势捕获 [Modifier] 的键值对。
 */
@Composable
internal fun rememberTitaniumTilt(
    enabled: Boolean,
    drag: TitaniumDrag,
    tuning: TitaniumCardTuning,
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

    val gesture = if (drag == TitaniumDrag.Disabled) Modifier else Modifier.pointerInput(drag, tuning.horizontalDragFraction, tuning.verticalDragFraction) {
        fun grip() { touch = motion.value.tilt * .45f }

        fun follow(delta: Offset) {
            val movement = Offset(
                delta.x / (size.width * tuning.horizontalDragFraction).coerceAtLeast(1f),
                delta.y / (size.height * tuning.verticalDragFraction).coerceAtLeast(1f),
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

/**
 * 监听设备重力感应器以提供倾斜向量。
 *
 * 在 Activity 处于活跃状态（Resumed）时注册系统重力传感器监听，自动根据当前屏幕旋转方向
 * （0°/90°/180°/270°）校准重力向量，并在页面暂停或销毁时注销监听并重置倾斜量。
 */
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

/**
 * 将二维偏移向量的模长限制在单位圆内（最大长度为 1.0）。
 */
private fun Offset.limitToUnitCircle(): Offset = this / getDistance().coerceAtLeast(1f)
