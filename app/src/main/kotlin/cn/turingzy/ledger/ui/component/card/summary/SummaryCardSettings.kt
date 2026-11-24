package cn.turingzy.ledger.ui.component.card.summary

import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.input.CompactSlider
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.model.settings.LedgerCardAppearance

/**
 * 摘要卡片背面设置面板组件。
 *
 * 位于卡片背面，提供以下外观调节功能：
 * - 毛玻璃透明度百分比滑动条（带当前百分比动态跟随浮标）。
 * - 更换/上传背景图片按钮。
 * - 底部空白区域继承水平滑动手势修饰符，便于用户滑动切回卡片正面。
 *
 * @param appearance 当前卡片外观配置。
 * @param enabled 设置控件是否处于可交互状态。
 * @param onUploadBackground 点击上传/更换背景图回调。
 * @param onTransparencyChange 滑动透明度滑块时的实时更新回调。
 * @param onTransparencyChangeFinished 停止拖动滑块后的落盘保存回调。
 * @param modifier 修饰符。
 * @param swipeModifier 供空白区域接收水平滑动手势以翻转卡片的修饰符。
 */
@Composable
internal fun SummaryCardSettings(
    appearance: LedgerCardAppearance,
    enabled: Boolean,
    onUploadBackground: () -> Unit,
    onTransparencyChange: (Float) -> Unit,
    onTransparencyChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    swipeModifier: Modifier = Modifier,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().height(52.dp).padding(start = 18.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(textResources.getString(cn.turingzy.ledger.R.string.ui_transparency), Modifier.width(52.dp), fontSize = LedgerStyle.typography.bodySmall.fontSize, color = colors.onPrimary)
            BoxWithConstraints(Modifier.weight(1f).height(40.dp)) {
                CompactSlider(
                    value = appearance.transparencyPercent.toFloat(),
                    onValueChange = onTransparencyChange,
                    onValueChangeFinished = onTransparencyChangeFinished,
                    enabled = enabled,
                    valueRange = 0f..100f,
                    color = colors.tertiary,
                    modifier = Modifier.align(Alignment.Center).fillMaxWidth().semantics { contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_transparency) },
                )
                Text(
                    "${appearance.transparencyPercent}%",
                    modifier = Modifier.offset(
                        x = (maxWidth - 40.dp).coerceAtLeast(0.dp) * (appearance.transparencyPercent / 100f),
                        y = (-2).dp,
                    ),
                    fontSize = LedgerStyle.typography.chartLabel.fontSize,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimary,
                )
            }
            SummaryCardIconButton(
                imageVector = MaterialSymbols.AddPhotoAlternate,
                contentDescription = textResources.getString(cn.turingzy.ledger.R.string.ui_change_card_background),
                onClick = onUploadBackground,
                enabled = enabled,
                modifier = Modifier.padding(start = LedgerStyle.spacing.medium),
            )
        }
        // The space below the controls still switches the card face on a horizontal swipe.
        Spacer(Modifier.fillMaxWidth().weight(1f).then(swipeModifier))
    }
}
