package cn.turingzy.ledger.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Book
import androidx.compose.ui.graphics.vector.ImageVector

object LedgerIcons {
    // Tab 1: 账本 (原型是经典的带书签横线书本 Book 图标)
    val LedgerOutline: ImageVector = Icons.Outlined.Book
    val LedgerFilled: ImageVector = Icons.Filled.Book

    // Tab 2: 统计 (原型是 3 根阶梯柱状图 BarChart 图标)
    val AnalyticsOutline: ImageVector = Icons.Outlined.BarChart
    val AnalyticsFilled: ImageVector = Icons.Filled.BarChart
}
