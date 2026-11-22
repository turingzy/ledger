package cn.turingzy.ledger.ui.feature.bill.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.ui.component.card.summary.SummaryCard
import cn.turingzy.ledger.ui.component.card.summary.SummaryCardContent

/**
 * 账单页面顶部的汇总卡片横幅组件。
 *
 * 显示当前统计周期的结余、支出与收入总额，支持金额显隐切换、自定义卡片背景图片及透明度调节。
 *
 * @param cardMonthDisplay 卡片顶部显示的标题文本（如 "11月 结余"）。
 * @param balance 本周期净结余金额（分）。
 * @param expense 本周期总支出金额（分）。
 * @param income 本周期总收入金额（分）。
 * @param modifier 外部修饰符。
 * @param appearance 卡片外观偏好设置 [LedgerCardAppearance]。
 * @param background 自定义背景图 [ImageBitmap]。
 * @param settingsEnabled 是否允许修改卡片外观与背景。
 * @param onUploadBackground 触发上传更换卡片背景图的回调。
 * @param onTransparencyChange 修改卡片遮罩透明度百分比的回调。
 * @param currency 显示货币代码，默认为 "CNY"。
 */
@Composable
fun SummaryHeroBanner(
    cardMonthDisplay: String,
    balance: Long,
    expense: Long,
    income: Long,
    modifier: Modifier = Modifier,
    appearance: LedgerCardAppearance = LedgerCardAppearance(),
    background: ImageBitmap? = null,
    settingsEnabled: Boolean = true,
    onUploadBackground: () -> Unit = {},
    onTransparencyChange: (Int) -> Unit = {},
    currency: String = "CNY",
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    var amountsHidden by rememberSaveable { mutableStateOf(false) }
    SummaryCard(
        modifier = modifier,
        appearance = appearance,
        background = background,
        settingsEnabled = settingsEnabled,
        onUploadBackground = onUploadBackground,
        onTransparencyChange = onTransparencyChange,
    ) {
        SummaryCardContent(
            title = cardMonthDisplay.ifBlank { textResources.getString(cn.turingzy.ledger.R.string.ui_balance) },
            amount = balance,
            currency = currency,
            amountsHidden = amountsHidden,
            onToggleAmounts = { amountsHidden = !amountsHidden },
            leftAmount = expense,
            rightAmount = income,
        )
    }
}
