package cn.turingzy.ledger.ui.component

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key

@Composable
internal fun rememberCircularCardPagerState(itemIds: List<Long?>, selectedId: Long?): PagerState = key(itemIds) {
    val count = itemIds.size.coerceAtLeast(1)
    val selectedIndex = itemIds.indexOf(selectedId).coerceAtLeast(0)
    rememberPagerState(
        initialPage = initialCircularCardPage(count, selectedIndex),
        pageCount = { if (count > 1) Int.MAX_VALUE else 1 },
    )
}

internal fun initialCircularCardPage(count: Int, selectedIndex: Int): Int {
    require(count > 0)
    require(selectedIndex in 0 until count)
    if (count == 1) return 0
    return (Int.MAX_VALUE / 2 / count) * count + selectedIndex
}
