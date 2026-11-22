package cn.turingzy.ledger.ui.component.card.bank

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import cn.turingzy.ledger.R
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.model.account.BuiltInCardFace

/**
 * 根据卡片模板 [CardTemplate] 获取对应的钛金属材质预设 [TitaniumPreset]。
 *
 * 若为内置卡面则返回对应的预设材质；若为自定义模板或 `null`，默认返回 [TitaniumPreset.IridescentLight]。
 */
internal fun CardTemplate?.titaniumPreset(): TitaniumPreset = when (this?.let(BuiltInCardFace::forTemplate)) {
    BuiltInCardFace.IridescentDark -> TitaniumPreset.IridescentDark
    BuiltInCardFace.PearlLight -> TitaniumPreset.PearlLight
    BuiltInCardFace.PearlDark -> TitaniumPreset.PearlDark
    BuiltInCardFace.IridescentLight, null -> TitaniumPreset.IridescentLight
}

/**
 * 获取卡片模板的展示标题。
 *
 * 若为默认命名的内置卡面，则返回对应的多语言本地化名称；否则返回用户自定义模板名称。
 */
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

/**
 * 解析卡片模板的封面背景图片 [Uri]。
 *
 * 仅当模板未被删除且协议为 "content"、"file" 或 "android.resource" 时有效。
 */
@Composable
internal fun CardTemplate?.titaniumImageUri(): Uri? = remember(this?.coverUri, this?.isDeleted) {
    this?.takeUnless { it.isDeleted }?.coverUri?.let(Uri::parse)
        ?.takeIf { it.scheme in setOf("content", "file", "android.resource") }
}

/**
 * 自动计算文字颜色的标识常量。
 */
internal const val AutomaticCardTextColor = "auto"

/**
 * 获取卡片模板中覆盖指定的文字颜色。
 *
 * 若为历史内置墨水配置、模板已删除或颜色无效，则返回 `null`（退化为自动亮度计算颜色）。
 */
internal fun CardTemplate?.titaniumInkOverride(): Color? {
    val template = this?.takeUnless { it.isDeleted } ?: return null
    if (BuiltInCardFace.forTemplate(template)?.usesLegacyInk(template) == true) return null
    return template.textColor.takeUnless { it.equals("#FFFFFF", ignoreCase = true) }?.asCardTextColor()
}

/**
 * 将十六进制颜色字符串（如 "#RRGGBB" 或 "#AARRGGBB"）解析为 Compose [Color]。
 *
 * @return 解析成功返回对应的 [Color]，若格式非法则返回 `null`。
 */
internal fun String.asCardTextColor(): Color? {
    val hex = removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    if (hex.any { it.digitToIntOrNull(16) == null }) return null
    val value = hex.toLongOrNull(16) ?: return null
    return Color((if (hex.length == 6) value or 0xFF000000L else value).toInt())
}
