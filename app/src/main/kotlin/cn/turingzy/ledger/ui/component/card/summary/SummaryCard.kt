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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import cn.turingzy.ledger.ui.theme.LedgerStyle
import kotlin.math.abs

/**
 * 账本首页摘要卡片容器组件。
 *
 * 支持通过横向滑动手势或无障碍动作在卡片正面（数据内容插槽）
 * 与卡片背面（背景图更换入口）之间平移切换。
 *
 * @param modifier 修饰符。
 * @param background 自定义卡片背景位图；为 `null` 时使用默认资源。
 * @param defaultBackgroundRes 默认背景 Drawable 资源 ID。
 * @param settingsEnabled 是否允许滑动切换至背面设置面板。
 * @param onBackgroundSelected 系统图片选择器返回图片后的回调。
 * @param content 卡片正面内容插槽，参数表示正面是否为当前卡面。
 */
@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    background: ImageBitmap? = null,
    @DrawableRes defaultBackgroundRes: Int = R.drawable.bg_ledger_card,
    settingsEnabled: Boolean = true,
    onBackgroundSelected: (Uri) -> Unit = {},
    content: @Composable BoxScope.(Boolean) -> Unit,
) {
    var settingsVisible by rememberSaveable { mutableStateOf(false) }
    var pickingBackground by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        pickingBackground = false
        if (uri != null) {
            settingsVisible = false
            onBackgroundSelected(uri)
        }
    }
    LifecycleResumeEffect(Unit) {
        // 系统选择器会暂停页面，选图取消时仍需回到卡片背面。
        onPauseOrDispose { if (!pickingBackground) settingsVisible = false }
    }
    var enterDirection by remember { mutableIntStateOf(1) }
    val transitionOffset = with(LocalDensity.current) { 32.dp.roundToPx() }
    val canSwitchFace = settingsEnabled || settingsVisible
    val switchFaceLabel = stringResource(
        if (settingsVisible) R.string.ui_close_card_settings else R.string.ui_card_settings,
    )
    val defaultPainter = painterResource(defaultBackgroundRes)
    val backgroundPainter = remember(background, defaultPainter) {
        background?.let(::BitmapPainter) ?: defaultPainter
    }
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
                    enabled = settingsEnabled && isActiveFace && !pickingBackground,
                    onUploadBackground = {
                        pickingBackground = true
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = swipeModifier,
                )
            } else {
                Box(Modifier.fillMaxSize().then(swipeModifier)) { content(isActiveFace) }
            }
        }
    }
}
