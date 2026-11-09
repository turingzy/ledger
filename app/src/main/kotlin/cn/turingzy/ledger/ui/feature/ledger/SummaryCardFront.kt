package cn.turingzy.ledger.ui.feature.ledger

import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.util.toFormattedAmount

@Composable
internal fun SummaryCardFront(
    cardMonthDisplay: String,
    balance: Long,
    expense: Long,
    income: Long,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp),
        ) {
            Text(
                text = cardMonthDisplay.ifBlank { "结余" },
                fontSize = 15.sp,
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
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = balance.toFormattedAmount(showSymbol = true, useGrouping = true),
                fontSize = 26.sp,
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
            Box(
                Modifier.fillMaxWidth().height(0.5.dp)
                    .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.32f)),
            )

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
                        text = "支出",
                        fontSize = 11.sp,
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
                        text = expense.toFormattedAmount(showSymbol = true, useGrouping = true),
                        fontSize = 16.sp,
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
                        text = "收入",
                        fontSize = 11.sp,
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
                        text = income.toFormattedAmount(showSymbol = true, useGrouping = true),
                        fontSize = 16.sp,
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
