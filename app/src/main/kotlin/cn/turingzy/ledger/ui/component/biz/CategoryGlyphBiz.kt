package cn.turingzy.ledger.ui.component.biz

import cn.turingzy.ledger.ui.component.MaterialSymbols
import androidx.compose.ui.graphics.vector.ImageVector
import cn.turingzy.ledger.model.category.Icon as CategoryIcon

/**
 * 将业务分类图标模型映射为对应的 Material Symbols 矢量图标。
 *
 * @return 对应的 [ImageVector] 图标；未知类型默认返回更多图标（[MaterialSymbols.MoreHoriz]）。
 */
internal fun CategoryIcon.categoryGlyphBiz(): ImageVector = when (keyName) {
    "ic_food" -> MaterialSymbols.Restaurant
    "ic_shopping" -> MaterialSymbols.ShoppingBag
    "ic_transport" -> MaterialSymbols.DirectionsBus
    "ic_entertainment" -> MaterialSymbols.SportsEsports
    "ic_housing" -> MaterialSymbols.Home
    "ic_medical" -> MaterialSymbols.LocalHospital
    "ic_salary" -> MaterialSymbols.Payments
    "ic_investment" -> MaterialSymbols.AccountBalance
    "ic_transfer" -> MaterialSymbols.SwapHoriz
    else -> MaterialSymbols.MoreHoriz
}
