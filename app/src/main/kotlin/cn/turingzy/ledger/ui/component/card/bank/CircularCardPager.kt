package cn.turingzy.ledger.ui.component.card.bank

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

/**
 * 创建并记住用于卡片无限循环滑动的 [PagerState]。
 *
 * 当卡片列表数量大于 1 时，通过将页数设为 [Int.MAX_VALUE]，并以居中倍数作为初始页偏移，
 * 模拟出双向无缝循环滑动的交互效果；卡片数量为 1 时退化为单页模式。
 *
 * @param itemIds 卡片标识符列表，用于作为状态缓存的 key 以及计算索引位置。
 * @param selectedId 当前选中的卡片标识符。
 * @return 绑定了循环初始页的 [PagerState]。
 */
@Composable
internal fun rememberCircularCardPagerState(itemIds: List<Long?>, selectedId: Long?): PagerState = key(itemIds) {
    val count = itemIds.size.coerceAtLeast(1)
    val selectedIndex = itemIds.indexOf(selectedId).coerceAtLeast(0)
    rememberPagerState(
        initialPage = initialCircularCardPage(count, selectedIndex),
        pageCount = { if (count > 1) Int.MAX_VALUE else 1 },
    )
}

/**
 * 计算循环卡片寻呼机的初始页面索引。
 *
 * 将初始页定位到 [Int.MAX_VALUE] 的中间区域，并确保对齐到 [selectedIndex]，
 * 使得用户在首次进入时即可向前或向后自由滑动。
 *
 * @param count 卡片总数量，必须大于 0。
 * @param selectedIndex 当前选中的卡片索引，必须在 `0 until count` 范围内。
 * @return 初始页码索引。
 */
internal fun initialCircularCardPage(count: Int, selectedIndex: Int): Int {
    require(count > 0)
    require(selectedIndex in 0 until count)
    if (count == 1) return 0
    return (Int.MAX_VALUE / 2 / count) * count + selectedIndex
}
