package cn.turingzy.ledger.ui.component

import androidx.compose.ui.graphics.vector.ImageVector
import cn.turingzy.ledger.model.Icon as CategoryIcon

internal fun CategoryIcon.categoryGlyph(): ImageVector = when (keyName) {
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
