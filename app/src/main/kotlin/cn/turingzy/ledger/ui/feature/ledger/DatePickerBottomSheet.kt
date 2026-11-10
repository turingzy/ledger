package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.ui.theme.IosBlue
import cn.turingzy.ledger.ui.theme.IosLabel
import cn.turingzy.ledger.ui.theme.IosSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerBottomSheet(
    initialYear: Int,
    initialMonth: Int,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onMonthSelected: (year: Int, month: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedYear by remember { mutableIntStateOf(initialYear) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "选择账本月份",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = IosLabel,
            )

            // 年份切换条 (< 2026年 >)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { selectedYear -= 1 }) {
                    Icon(
                        imageVector = MaterialSymbols.ArrowBack,
                        contentDescription = "上一年",
                        tint = IosLabel,
                    )
                }

                Text(
                    text = "${selectedYear}年",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = IosLabel,
                )

                IconButton(onClick = { selectedYear += 1 }) {
                    Icon(
                        imageVector = MaterialSymbols.ArrowForward,
                        contentDescription = "下一年",
                        tint = IosLabel,
                    )
                }
            }

            // 1~12 月网格选择
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items((1..12).toList()) { m ->
                    val isSelected = selectedYear == initialYear && m == initialMonth
                    val bgColor = if (isSelected) IosBlue else Color(0xFFF2F2F7)
                    val textColor = if (isSelected) Color.White else IosLabel

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(bgColor)
                            .clickable {
                                onMonthSelected(selectedYear, m)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${m}月",
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor,
                        )
                    }
                }
            }
        }
    }
}
