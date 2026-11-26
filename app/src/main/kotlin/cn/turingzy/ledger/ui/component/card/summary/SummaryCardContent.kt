package cn.turingzy.ledger.ui.component.card.summary

import cn.turingzy.ledger.ui.component.MaterialSymbols
import cn.turingzy.ledger.ui.component.biz.currencySymbolBiz
import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState

/**
 * 摘要卡片正面内容展示组件。
 *
 * 布局分为上下两个区域：
 * - 上部：展示卡片主标题、金额明文/密文切换按钮以及核心汇总金额。
 * - 底部毛玻璃区域：左右两侧分别展示子项标签与对应金额（例如支出与收入）。
 * 文字均附加阴影效果以保证在各类明暗背景图片下的清晰可读性。
 *
 * @param title 卡片主标题（例如“本月结余”或“总资产”）。
 * @param amount 核心主金额（以“分”为单位）。
 * @param leftAmount 左侧子项金额（以“分”为单位，例如支出金额）。
 * @param rightAmount 右侧子项金额（以“分”为单位，例如收入金额）。
 * @param amountsHidden 当前金额是否处于脱敏隐藏状态。
 * @param onToggleAmounts 点击显隐切换按钮时的回调。
 * @param modifier 修饰符。
 * @param leftLabel 左侧子项标签文本（默认为“支出”）。
 * @param rightLabel 右侧子项标签文本（默认为“收入”）。
 * @param currency 货币代号（默认为 "CNY"），用于确定货币符号。
 * @param activeFace 正面是否为当前卡面。
 */
@Composable
fun SummaryCardContent(
    title: String,
    amount: Long,
    leftAmount: Long,
    rightAmount: Long,
    amountsHidden: Boolean,
    onToggleAmounts: () -> Unit,
    modifier: Modifier = Modifier,
    leftLabel: String = androidx.compose.ui.res.stringResource(cn.turingzy.ledger.R.string.ui_expense),
    rightLabel: String = androidx.compose.ui.res.stringResource(cn.turingzy.ledger.R.string.ui_income),
    currency: String = "CNY",
    activeFace: Boolean = true,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val currencySymbol = currencySymbolBiz(currency)
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val animateAmounts = activeFace && lifecycleState.isAtLeast(Lifecycle.State.RESUMED)
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = LedgerStyle.spacing.cardPadding, end = LedgerStyle.spacing.cardPadding, top = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    fontSize = LedgerStyle.typography.transactionTitle.fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.50f),
                            offset = androidx.compose.ui.geometry.Offset(0f, 1.5f),
                            blurRadius = 6f,
                        ),
                    ),
                )
                SummaryCardIconButton(
                    imageVector = if (amountsHidden) MaterialSymbols.VisibilityOff else MaterialSymbols.Visibility,
                    contentDescription = if (amountsHidden) textResources.getString(cn.turingzy.ledger.R.string.ui_show_amounts) else textResources.getString(cn.turingzy.ledger.R.string.ui_hide_amounts),
                    onClick = onToggleAmounts,
                    modifier = Modifier.size(LedgerStyle.icons.default),
                    containerColor = Color.Transparent,
                    iconSize = LedgerStyle.icons.small,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            SummaryAmountTicker(
                amount = amount,
                currencySymbol = currencySymbol,
                hidden = amountsHidden,
                animate = animateAmounts,
                style = TextStyle(
                    fontSize = LedgerStyle.typography.heroAmount.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    letterSpacing = (-0.6).sp,
                    shadow = androidx.compose.ui.graphics.Shadow(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                        offset = androidx.compose.ui.geometry.Offset(0f, 3f),
                        blurRadius = 12f,
                    ),
                ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .align(Alignment.BottomCenter),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = LedgerStyle.spacing.cardPadding),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = leftLabel,
                        fontSize = LedgerStyle.typography.caption.fontSize,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.70f),
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.70f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 1.5f),
                                blurRadius = 4f,
                            ),
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    SummaryAmountTicker(
                        amount = leftAmount,
                        currencySymbol = currencySymbol,
                        hidden = amountsHidden,
                        animate = animateAmounts,
                        style = TextStyle(
                            fontSize = LedgerStyle.typography.body.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                blurRadius = 6f,
                            ),
                        ),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = rightLabel,
                        fontSize = LedgerStyle.typography.caption.fontSize,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.70f),
                        style = TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.70f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 1.5f),
                                blurRadius = 4f,
                            ),
                        ),
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    SummaryAmountTicker(
                        amount = rightAmount,
                        currencySymbol = currencySymbol,
                        hidden = amountsHidden,
                        animate = animateAmounts,
                        style = TextStyle(
                            fontSize = LedgerStyle.typography.body.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary,
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f),
                                offset = androidx.compose.ui.geometry.Offset(0f, 2f),
                                blurRadius = 6f,
                            ),
                        ),
                    )
                }
            }
        }
    }
}
