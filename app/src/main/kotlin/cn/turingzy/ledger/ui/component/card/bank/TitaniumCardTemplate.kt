package cn.turingzy.ledger.ui.component.card.bank

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.BuiltInCardFace

internal fun CardTemplate?.titaniumPreset(): TitaniumPreset = when (this?.let(BuiltInCardFace::forTemplate)) {
    BuiltInCardFace.IridescentDark -> TitaniumPreset.IridescentDark
    BuiltInCardFace.PearlLight -> TitaniumPreset.PearlLight
    BuiltInCardFace.PearlDark -> TitaniumPreset.PearlDark
    BuiltInCardFace.IridescentLight, null -> TitaniumPreset.IridescentLight
}

@Composable
internal fun CardTemplate?.titaniumTitle(): String {
    val face = if (this == null) BuiltInCardFace.IridescentLight else BuiltInCardFace.forTemplate(this)
        ?.takeIf { it.hasDefaultName(this) }
    return when (face) {
        BuiltInCardFace.IridescentLight -> stringResource(R.string.ui_card_iridescent)
        BuiltInCardFace.IridescentDark -> stringResource(R.string.ui_card_iridescent_dark)
        BuiltInCardFace.PearlLight -> stringResource(R.string.ui_card_pearl_light)
        BuiltInCardFace.PearlDark -> stringResource(R.string.ui_card_pearl_dark)
        null -> requireNotNull(this).name
    }
}

@Composable
internal fun CardTemplate?.titaniumImageUri(): Uri? = remember(this?.coverUri, this?.isDeleted) {
    this?.takeUnless { it.isDeleted }?.coverUri?.let(Uri::parse)
        ?.takeIf { it.scheme in setOf("content", "file", "android.resource") }
}

internal const val AutomaticCardTextColor = "auto"

internal fun CardTemplate?.titaniumInkOverride(): Color? {
    val template = this?.takeUnless { it.isDeleted } ?: return null
    if (BuiltInCardFace.forTemplate(template)?.usesLegacyInk(template) == true) return null
    return template.textColor.takeUnless { it.equals("#FFFFFF", ignoreCase = true) }?.asCardTextColor()
}

internal fun String.asCardTextColor(): Color? {
    val hex = removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    if (hex.any { it.digitToIntOrNull(16) == null }) return null
    val value = hex.toLongOrNull(16) ?: return null
    return Color((if (hex.length == 6) value or 0xFF000000L else value).toInt())
}
