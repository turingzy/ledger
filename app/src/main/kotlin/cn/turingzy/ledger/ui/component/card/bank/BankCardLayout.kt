package cn.turingzy.ledger.ui.component.card.bank

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * 银行卡标准宽高比（符合 ISO/IEC 7810 ID-1 标准，约 85.60mm / 53.98mm）。
 */
internal const val BankCardAspectRatio = 1.586f

/**
 * 卡片分页指示器组件。
 *
 * 用于在多张银行卡横向切换时展示当前页码及总页数圆点。
 *
 * @param pageCount 总页数（卡片总数）。
 * @param currentPage 当前选中的页码索引（从 0 开始）。
 * @param modifier 修饰符。
 */
@Composable
internal fun CardPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    val textResources = androidx.compose.ui.platform.LocalContext.current.resources
    val colors = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().semantics {
            stateDescription = if (pageCount == 0) textResources.getString(cn.turingzy.ledger.R.string.ui_no_cards) else "第${currentPage + 1}张，共${pageCount}张卡片"
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        repeat(pageCount) { index ->
            Box(
                Modifier.size(6.dp).clip(CircleShape)
                    .background(if (index == currentPage) colors.onSurface else colors.outlineVariant),
            )
        }
    }
}
