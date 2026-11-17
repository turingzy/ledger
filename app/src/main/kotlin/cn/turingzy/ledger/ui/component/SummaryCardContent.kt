package cn.turingzy.ledger.ui.component

import cn.turingzy.ledger.ui.theme.LedgerStyle
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.util.toFormattedAmount

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
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val currencySymbol = currencySymbol(currency)
    val visibilityIconSize = with(LocalDensity.current) { 15.sp.toDp() }
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 10.dp),
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
                    iconSize = visibilityIconSize,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (amountsHidden) "****" else amount.toFormattedAmount(showSymbol = true, symbol = currencySymbol, useGrouping = true),
                fontSize = LedgerStyle.typography.heroAmount.fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                letterSpacing = (-0.6).sp,
                style = TextStyle(
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
                    .padding(horizontal = 18.dp),
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
                    Text(
                        text = if (amountsHidden) "****" else leftAmount.toFormattedAmount(showSymbol = true, symbol = currencySymbol, useGrouping = true),
                        fontSize = LedgerStyle.typography.body.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = TextStyle(
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
                    Text(
                        text = if (amountsHidden) "****" else rightAmount.toFormattedAmount(showSymbol = true, symbol = currencySymbol, useGrouping = true),
                        fontSize = LedgerStyle.typography.body.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        style = TextStyle(
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
