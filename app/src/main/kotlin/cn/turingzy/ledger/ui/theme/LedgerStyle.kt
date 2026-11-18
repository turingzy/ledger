package cn.turingzy.ledger.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp

object LedgerStyle {
    val colors: LedgerSemanticColors
        @Composable get() {
            val scheme = MaterialTheme.colorScheme
            return remember(scheme) { ledgerSemanticColors(scheme) }
        }

    val typography = LedgerTypography
    val shapes = LedgerShapes
    val spacing = LedgerSpacing
    val dimensions = LedgerDimensions
    val opacity = LedgerOpacity
    val icons = LedgerIconStyle
    val decorationColors = LedgerDecorationColors
}

object LedgerIconStyle {
    const val family = "Material Symbols"
    const val variant = "Rounded"
    const val weight = 400
    const val grade = 0
    const val opticalSize = 24

    val dropdownCheck = 18.dp
    val default = 24.dp
    val inline = 20.dp
    val small = 16.dp
    val externalLink = 14.dp
    val trend = 10.dp
    val navigation = 25.dp
    val back = 32.dp
    val imagePlaceholder = 40.dp
}

object LedgerSpacing {
    val none = 0.dp
    val hairline = 1.dp
    val tiny = 2.dp
    val extraSmall = 4.dp
    val small = 6.dp
    val medium = 8.dp
    val large = 12.dp
    val extraLarge = 16.dp
    val section = 20.dp
    val block = 24.dp
    val page = 32.dp

    val pageHorizontal = section
    val cardPadding = extraLarge
    val rowHorizontal = large
    val rowVertical = medium
    val inlineGap = medium
    val sectionGap = large
    val labelGap = extraSmall
    val settingsIconGap = 14.dp
    val calendarCardHorizontal = 14.dp
    val calendarCardVertical = 10.dp
}

object LedgerDimensions {
    val formBorderThickness = 1.dp
    val fieldLabelOffset = 10.dp
    val formLabelWidth = 96.dp
    val dialogMaxHeight = 520.dp
    val sheetMaxHeight = 640.dp
    val wideDialogMaxWidth = 560.dp
    val compactToolbarHeight = 44.dp
    val supportingRowMinHeight = 58.dp
    val dropdownWidth = 160.dp
    val dropdownMaxHeight = 200.dp
    val dropdownItemHeight = 36.dp
    val dropdownShadow = 6.dp
    val dropdownBorder = 1.dp
    val listIconContainer = 32.dp
    val settingsDividerInset = 58.dp
    val subtleCardShadow = 2.dp
    val touchTarget = 48.dp
    val compactIconButton = 36.dp
    val backButton = 44.dp
    val toolbarHeight = 56.dp
    val rowMinHeight = 48.dp
    val dividerThickness = 0.5.dp
    val topBarBlur = 3.dp
    val topBarFade = 4.dp
    val transactionRowHorizontal = 14.dp
    val compactButtonHeight = 36.dp
    val calendarEmptyIllustrationWidth = 140.dp
    val calendarEmptyIllustrationHeight = 105.dp
}

object LedgerOpacity {
    const val readOnlyBorder = 0.12f
    const val subtleCardShadow = 0.04f
    const val subduedChevron = 0.35f
    const val disabled = 0.38f
    const val pressed = 0.05f
    const val toolbarPressed = 0.12f
    const val selectedBackground = 0.08f
    const val cardIconBackground = 0.24f
    const val topBarScrolled = 0.55f
}

object LedgerShapes {
    val indicator = RoundedCornerShape(3.dp)
    val chartTooltip = RoundedCornerShape(5.dp)
    val control = RoundedCornerShape(8.dp)
    val field = RoundedCornerShape(12.dp)
    val card = RoundedCornerShape(16.dp)
    val bankCardRadius = 18.dp
    val bankCard = RoundedCornerShape(bankCardRadius)
    val summaryCard = RoundedCornerShape(20.dp)
    val dialog = RoundedCornerShape(24.dp)
    val confirmationDialog = RoundedCornerShape(28.dp)
    val bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val pill = CircleShape

    val material = Shapes(
        extraSmall = control,
        small = field,
        medium = card,
        large = summaryCard,
        extraLarge = confirmationDialog,
    )
}

object LedgerInteractionStyle {
    const val clickDebounceMillis = 350L
    const val pressEnterMillis = 70
    const val quickTapEnterMillis = 40
    const val quickTapHoldMillis = 40L
    const val pressExitMillis = 140
    const val pressCancelMillis = 100
}
