package cn.turingzy.ledger.ui.feature.bill.component

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import cn.turingzy.ledger.ui.component.card.summary.SummaryCard
import cn.turingzy.ledger.ui.component.card.summary.SummaryCardContent

/**
 * 账单页面顶部的汇总卡片横幅组件。
 *
 * 显示当前统计周期的结余、支出与收入总额，支持金额显隐切换和自定义卡片背景图片。
 *
 * @param cardMonthDisplay 卡片顶部显示的标题文本（如 "11月 结余"）。
 * @param balance 本周期净结余金额（分）。
 * @param expense 本周期总支出金额（分）。
 * @param income 本周期总收入金额（分）。
 * @param modifier 外部修饰符。
 * @param background 自定义背景图 [ImageBitmap]。
 * @param settingsEnabled 是否允许修改卡片外观与背景。
 * @param onBackgroundSelected 选中卡片背景图片后的回调。
 * @param currency 显示货币代码，默认为 "CNY"。
 * @param amountPeriodKey 统计周期变化时重新建立金额动画基准。
 */
@Composable
fun SummaryHeroBanner(
    cardMonthDisplay: String,
    balance: Long,
    expense: Long,
    income: Long,
    modifier: Modifier = Modifier,
    background: ImageBitmap? = null,
    settingsEnabled: Boolean = true,
    onBackgroundSelected: (Uri) -> Unit = {},
    currency: String = "CNY",
    amountPeriodKey: Any = cardMonthDisplay,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var amountsHidden by rememberSaveable { mutableStateOf(false) }
    SummaryCard(
        modifier = modifier,
        background = background,
        settingsEnabled = settingsEnabled,
        onBackgroundSelected = onBackgroundSelected,
    ) { activeFace ->
        key(currency, amountPeriodKey) {
            SummaryCardContent(
                title = cardMonthDisplay.ifBlank { textResources.getString(cn.turingzy.ledger.R.string.ui_balance) },
                amount = balance,
                currency = currency,
                amountsHidden = amountsHidden,
                onToggleAmounts = { amountsHidden = !amountsHidden },
                leftAmount = expense,
                rightAmount = income,
                activeFace = activeFace,
            )
        }
    }
}
