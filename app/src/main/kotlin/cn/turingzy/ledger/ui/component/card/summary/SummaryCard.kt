package cn.turingzy.ledger.ui.component.card.summary

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.ui.component.base.rememberBlurredBackdropBase
import cn.turingzy.ledger.ui.theme.LedgerStyle
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 账本首页摘要卡片容器组件。
 *
 * 具有毛玻璃质感底栏、背景图阴影与边框，支持通过横向滑动手势或无障碍动作在卡片正面（数据内容插槽）
 * 与卡片背面（卡片外观设置：背景图上传与毛玻璃透明度调节）之间进行滑动翻转切换。
 *
 * @param modifier 修饰符。
 * @param appearance 卡片外观配置（包含透明度等）。
 * @param background 自定义卡片背景位图；为 `null` 时使用默认资源。
 * @param defaultBackgroundRes 默认背景 Drawable 资源 ID。
 * @param settingsEnabled 是否允许滑动切换至背面设置面板。
 * @param onUploadBackground 点击上传/更换背景图回调。
 * @param onTransparencyChange 毛玻璃透明度百分比调节完成回调。
 * @param content 卡片正面内容插槽，参数表示正面是否为当前卡面。
 */
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    appearance: LedgerCardAppearance = LedgerCardAppearance(),
    background: ImageBitmap? = null,
    @DrawableRes defaultBackgroundRes: Int = R.drawable.bg_ledger_card,
    settingsEnabled: Boolean = true,
    onUploadBackground: () -> Unit = {},
    onTransparencyChange: (Int) -> Unit = {},
    content: @Composable BoxScope.(Boolean) -> Unit,
) {
    var settingsVisible by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        onPauseOrDispose { settingsVisible = false }
    }
    var enterDirection by remember { mutableIntStateOf(1) }
    val transitionOffset = with(LocalDensity.current) { 32.dp.roundToPx() }
    val canSwitchFace = settingsEnabled || settingsVisible
    val switchFaceLabel = stringResource(
        if (settingsVisible) R.string.ui_close_card_settings else R.string.ui_card_settings,
    )
    var previewPercent by remember(appearance.transparencyPercent) {
        mutableFloatStateOf(appearance.transparencyPercent.toFloat())
    }
    val previewAppearance = appearance.copy(transparencyPercent = previewPercent.roundToInt())
    val defaultPainter = painterResource(defaultBackgroundRes)
    val backgroundPainter = remember(background, defaultPainter) {
        background?.let(::BitmapPainter) ?: defaultPainter
    }
    val blurredBackdrop = rememberBlurredBackdropBase(defaultBackgroundRes, background)
    val colors = MaterialTheme.colorScheme
    val shape = LedgerStyle.shapes.summaryCard

    Box(
        modifier.fillMaxWidth().height(132.dp)
            .shadow(12.dp, shape, spotColor = colors.scrim.copy(alpha = 0.18f))
            .clip(shape)
            .border(0.5.dp, colors.onPrimary.copy(alpha = 0.25f), shape)
            .semantics {
                if (canSwitchFace) {
                    customActions = listOf(CustomAccessibilityAction(switchFaceLabel) {
                        enterDirection = if (settingsVisible) -1 else 1
                        settingsVisible = !settingsVisible
                        true
                    })
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            backgroundPainter, null, Modifier.fillMaxSize(),
            alignment = BiasAlignment(0f, -0.2f), contentScale = ContentScale.Crop,
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        colors.scrim.copy(alpha = 0.22f),
                        colors.scrim.copy(alpha = 0.04f),
                        colors.scrim.copy(alpha = 0.10f),
                    ),
                ),
            ),
        )
        Box(Modifier.fillMaxWidth().height(52.dp).align(Alignment.BottomCenter)) {
            SummaryCardGlass(previewAppearance, backgroundPainter, blurredBackdrop, showDivider = false)
        }
        AnimatedContent(
            targetState = settingsVisible,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val offset = transitionOffset * enterDirection
                (slideInHorizontally(tween(240)) { offset } + fadeIn(tween(220))) togetherWith
                    (slideOutHorizontally(tween(240)) { -offset } + fadeOut(tween(180)))
            },
            label = "summaryCardSide",
        ) { showingSettings ->
            val isActiveFace = showingSettings == settingsVisible
            val swipeModifier = if (isActiveFace && canSwitchFace) Modifier.pointerInput(showingSettings) {
                var distance = 0f
                detectHorizontalDragGestures(
                    onDragStart = { distance = 0f },
                    onDragEnd = {
                        if (abs(distance) >= 24.dp.toPx()) {
                            enterDirection = if (distance < 0f) 1 else -1
                            settingsVisible = !showingSettings
                        }
                    },
                    onHorizontalDrag = { _, delta -> distance += delta },
                )
            } else Modifier
            if (showingSettings) {
                SummaryCardSettings(
                    appearance = previewAppearance,
                    enabled = settingsEnabled && isActiveFace,
                    onUploadBackground = onUploadBackground,
                    onTransparencyChange = { previewPercent = it },
                    onTransparencyChangeFinished = { onTransparencyChange(previewPercent.roundToInt()) },
                    modifier = Modifier.fillMaxSize(),
                    swipeModifier = swipeModifier,
                )
            } else {
                Box(Modifier.fillMaxSize().then(swipeModifier)) { content(isActiveFace) }
            }
        }
    }
}


/**
 * 记住并返回用于选择摘要卡自定义背景图片的启动器回调。
 *
 * 封装了系统照片选择器 [ActivityResultContracts.PickVisualMedia]，仅筛选图片类型。
 *
 * @param onImage 选中图片后的 URI 回调。
 * @return 触发图片选择器的无参函数。
 */
@Composable
internal fun rememberSummaryBackgroundPicker(onImage: (Uri) -> Unit): () -> Unit {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onImage(uri)
    }
    return { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
