package cn.turingzy.ledger.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.vector.ImageVector
import cn.turingzy.ledger.model.Icon as CategoryIcon
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SportsEsports

internal fun CategoryIcon.categoryGlyph(): ImageVector = when (keyName) {
    "ic_food" -> Icons.Outlined.Restaurant
    "ic_shopping" -> Icons.Outlined.ShoppingBag
    "ic_transport" -> Icons.Outlined.DirectionsBus
    "ic_entertainment" -> Icons.Outlined.SportsEsports
    "ic_housing" -> Icons.Outlined.Home
    "ic_medical" -> Icons.Outlined.LocalHospital
    "ic_salary" -> Icons.Outlined.Payments
    "ic_investment" -> Icons.Outlined.AccountBalance
    "ic_transfer" -> Icons.Outlined.SwapHoriz
    else -> Icons.Outlined.MoreHoriz
}
