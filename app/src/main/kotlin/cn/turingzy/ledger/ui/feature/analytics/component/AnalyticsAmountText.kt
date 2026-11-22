package cn.turingzy.ledger.ui.feature.analytics.component

import cn.turingzy.ledger.ui.component.biz.LocalCurrencyBiz
import cn.turingzy.ledger.ui.component.biz.AmountTextBiz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * 统计分析模块专用的金额显示文本组件。
 *
 * 自动从 [LocalCurrencyBiz] 获取当前币种，并应用等宽数字特征排版。
 *
 * @param amount 金额数值（分）。
 * @param modifier 修饰符。
 * @param textAlign 文本对齐方式。
 * @param fontSize 字体大小。
 */
@Composable
internal fun AnalyticsAmountText(amount: Long, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp) {
    AmountTextBiz(
        amount, LocalCurrencyBiz.current, modifier,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontSize = fontSize, lineHeight = 18.sp, fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface, fontFeatureSettings = "tnum", textAlign = textAlign,
        ),
    )
}
