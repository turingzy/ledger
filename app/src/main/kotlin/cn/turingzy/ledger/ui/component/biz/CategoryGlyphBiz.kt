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
    "ic_category_grocery" -> MaterialSymbols.Grocery
    "ic_category_local_cafe" -> MaterialSymbols.LocalCafe
    "ic_category_shopping_cart" -> MaterialSymbols.ShoppingCart
    "ic_category_key" -> MaterialSymbols.Key
    "ic_category_bolt" -> MaterialSymbols.Bolt
    "ic_category_wifi" -> MaterialSymbols.Wifi
    "ic_category_school" -> MaterialSymbols.School
    "ic_category_fitness_center" -> MaterialSymbols.FitnessCenter
    "ic_category_pets" -> MaterialSymbols.Pets
    "ic_category_flight_takeoff" -> MaterialSymbols.FlightTakeoff
    "ic_category_build" -> MaterialSymbols.Build
    "ic_category_local_gas_station" -> MaterialSymbols.LocalGasStation
    "ic_category_checkroom" -> MaterialSymbols.Checkroom
    "ic_category_spa" -> MaterialSymbols.Spa
    "ic_category_verified_user" -> MaterialSymbols.VerifiedUser
    "ic_category_redeem" -> MaterialSymbols.Redeem
    "ic_category_military_tech" -> MaterialSymbols.MilitaryTech
    "ic_category_celebration" -> MaterialSymbols.Celebration
    "ic_category_more_time" -> MaterialSymbols.MoreTime
    "ic_category_work" -> MaterialSymbols.Work
    "ic_category_storefront" -> MaterialSymbols.Storefront
    "ic_category_volunteer_activism" -> MaterialSymbols.VolunteerActivism
    "ic_category_receipt_long" -> MaterialSymbols.ReceiptLong
    "ic_category_assignment_return" -> MaterialSymbols.AssignmentReturn
    "ic_category_percent" -> MaterialSymbols.Percent
    "ic_category_mail" -> MaterialSymbols.Mail
    "ic_category_apartment" -> MaterialSymbols.Apartment
    "ic_category_savings" -> MaterialSymbols.Savings
    "ic_category_pie_chart" -> MaterialSymbols.PieChart
    "ic_category_sell" -> MaterialSymbols.Sell
    "ic_category_edit_note" -> MaterialSymbols.EditNote
    "ic_category_elderly" -> MaterialSymbols.Elderly
    "ic_category_add_circle" -> MaterialSymbols.AddCircle
    else -> MaterialSymbols.MoreHoriz
}
