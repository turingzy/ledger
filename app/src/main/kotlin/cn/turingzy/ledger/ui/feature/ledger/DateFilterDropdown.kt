package cn.turingzy.ledger.ui.feature.ledger

import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.turingzy.ledger.ui.theme.IosBlue
import cn.turingzy.ledger.ui.theme.IosLabel
import cn.turingzy.ledger.ui.theme.IosSecondary
import cn.turingzy.ledger.ui.theme.IosSeparator
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun DateFilterDropdown(
    expanded: Boolean,
    currentFilterType: DateFilterType,
    currentYear: Int,
    currentMonth: Int,
    onDismissRequest: () -> Unit,
    onSelectWeek: (LocalDate) -> Unit,
    onSelectMonth: (year: Int, month: Int) -> Unit,
    onSelectYear: (year: Int) -> Unit,
    onSelectRange: (startDate: LocalDate, endDate: LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember(currentFilterType) { mutableStateOf(currentFilterType) }
    var yearStepper by remember(currentYear) { mutableIntStateOf(currentYear) }
    var weekAnchor by remember { mutableStateOf(LocalDate.now()) }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(x = 0.dp, y = 6.dp),
        modifier = modifier
            .width(286.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = Color.Black.copy(alpha = 0.18f))
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(0.5.dp, Color(0x1F000000), RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 顶部分段控制胶囊: [ 周 · 月 · 年 · 区间 ]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(Color(0xFFE5E5EA))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                DateFilterType.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    val label = when (tab) {
                        DateFilterType.WEEK -> "周"
                        DateFilterType.MONTH -> "月"
                        DateFilterType.YEAR -> "年"
                        DateFilterType.RANGE -> "区间"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { selectedTab = tab },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) IosLabel else IosSecondary,
                        )
                    }
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = IosSeparator)

            // 各分段对应选择内容 (纯标准组件，无 SubcomposeLayout，彻底杜绝崩溃)
            when (selectedTab) {
                DateFilterType.WEEK -> {
                    WeekFilterPanel(
                        currentAnchor = weekAnchor,
                        onAnchorChange = { weekAnchor = it },
                        onConfirm = {
                            onSelectWeek(weekAnchor)
                            onDismissRequest()
                        },
                    )
                }
                DateFilterType.MONTH -> {
                    MonthFilterPanel(
                        selectedYear = yearStepper,
                        activeYear = currentYear,
                        activeMonth = currentMonth,
                        onYearChange = { yearStepper = it },
                        onMonthPicked = { m ->
                            onSelectMonth(yearStepper, m)
                            onDismissRequest()
                        },
                    )
                }
                DateFilterType.YEAR -> {
                    YearFilterPanel(
                        currentYear = currentYear,
                        onYearPicked = { y ->
                            onSelectYear(y)
                            onDismissRequest()
                        },
                    )
                }
                DateFilterType.RANGE -> {
                    RangeFilterPanel(
                        onPresetPicked = { start, end ->
                            onSelectRange(start, end)
                            onDismissRequest()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekFilterPanel(
    currentAnchor: LocalDate,
    onAnchorChange: (LocalDate) -> Unit,
    onConfirm: () -> Unit,
) {
    val startOfWeek = currentAnchor.with(DayOfWeek.MONDAY)
    val endOfWeek = currentAnchor.with(DayOfWeek.SUNDAY)
    val now = LocalDate.now()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 上周 / 下周 Stepper
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onAnchorChange(currentAnchor.minusWeeks(1)) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.ArrowBack,
                    contentDescription = "上一周",
                    tint = IosLabel,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = "${startOfWeek.monthValue}.${startOfWeek.dayOfMonth} - ${endOfWeek.monthValue}.${endOfWeek.dayOfMonth}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = IosLabel,
            )

            IconButton(
                onClick = { onAnchorChange(currentAnchor.plusWeeks(1)) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.ArrowForward,
                    contentDescription = "下一周",
                    tint = IosLabel,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // 快捷选项
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF2F2F7))
                    .clickable { onAnchorChange(now) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "本周", fontSize = 12.sp, color = IosLabel, fontWeight = FontWeight.Medium)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF2F2F7))
                    .clickable { onAnchorChange(now.minusWeeks(1)) },
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "上周", fontSize = 12.sp, color = IosLabel, fontWeight = FontWeight.Medium)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IosBlue)
                .clickable { onConfirm() },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "应用此周", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun MonthFilterPanel(
    selectedYear: Int,
    activeYear: Int,
    activeMonth: Int,
    onYearChange: (Int) -> Unit,
    onMonthPicked: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 年份步进器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onYearChange(selectedYear - 1) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.ArrowBack,
                    contentDescription = "上一年",
                    tint = IosLabel,
                    modifier = Modifier.size(16.dp),
                )
            }

            Text(
                text = "${selectedYear}年",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = IosLabel,
            )

            IconButton(
                onClick = { onYearChange(selectedYear + 1) },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.ArrowForward,
                    contentDescription = "下一年",
                    tint = IosLabel,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // 3 行 x 4 列标准月份网格 (无 LazyLayout，测量安全稳定)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (row in 0..2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    for (col in 1..4) {
                        val m = row * 4 + col
                        val isSelected = (selectedYear == activeYear) && (m == activeMonth)
                        val bg = if (isSelected) IosBlue else Color(0xFFF2F2F7)
                        val textCol = if (isSelected) Color.White else IosLabel

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .clickable { onMonthPicked(m) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "${m}月",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textCol,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearFilterPanel(
    currentYear: Int,
    onYearPicked: (Int) -> Unit,
) {
    val years = remember {
        listOf(currentYear - 2, currentYear - 1, currentYear, currentYear + 1)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        years.forEach { y ->
            val isSelected = y == currentYear
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) IosBlue else Color(0xFFF2F2F7))
                    .clickable { onYearPicked(y) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${y}年度账本",
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else IosLabel,
                )
            }
        }
    }
}

@Composable
private fun RangeFilterPanel(
    onPresetPicked: (LocalDate, LocalDate) -> Unit,
) {
    val today = remember { LocalDate.now() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val presets = listOf(
            "今天" to (today to today),
            "近 7 天" to (today.minusDays(6) to today),
            "近 30 天" to (today.minusDays(29) to today),
            "本月" to (today.withDayOfMonth(1) to today),
            "上月" to (today.minusMonths(1).withDayOfMonth(1) to today.minusMonths(1).let { it.withDayOfMonth(it.lengthOfMonth()) }),
        )

        presets.forEach { (title, range) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF2F2F7))
                    .clickable { onPresetPicked(range.first, range.second) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = IosLabel)
                    Text(
                        text = "${range.first.monthValue}.${range.first.dayOfMonth} - ${range.second.monthValue}.${range.second.dayOfMonth}",
                        fontSize = 11.sp,
                        color = IosSecondary,
                    )
                }
            }
        }
    }
}
