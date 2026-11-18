package cn.turingzy.ledger.model.account

internal enum class BuiltInCardFace(
    val id: Long,
    private val nameZh: String,
    private val coverUri: String,
    private val legacyName: String? = null,
    private val legacyUri: String? = null,
    private val legacyInk: String = "#FFFFFF",
) {
    IridescentLight(-104L, "流光钛金", "res://card-face/iridescent"),
    IridescentDark(-103L, "暮光钛金", "res://card-face/twilight", "商务黑金", "res://drawable/card_black_gold", "#D4AF37"),
    PearlLight(-102L, "静谧珠光", "res://card-face/pearl", "活力薄荷", "res://drawable/card_mint_green"),
    PearlDark(-101L, "深海珠光", "res://card-face/deep-sea", "经典深蓝", "res://drawable/card_classic_blue");

    fun asTemplate(appearance: CardTemplate? = null) = CardTemplate(
        id = id,
        name = appearance?.name?.takeIf { it.isNotBlank() } ?: nameZh,
        coverUri = coverUri,
        textColor = appearance?.textColor ?: "#FFFFFF",
        isSystem = true,
    )

    fun hasDefaultName(template: CardTemplate): Boolean =
        if (template.id == id) template.name == nameZh else isLegacyDefault(template)

    fun isLegacyDefault(template: CardTemplate): Boolean =
        isLegacyCover(template) && template.name == legacyName && usesLegacyInk(template)

    fun usesLegacyInk(template: CardTemplate): Boolean =
        isLegacyCover(template) && template.textColor.equals(legacyInk, ignoreCase = true)

    private fun isLegacyCover(template: CardTemplate): Boolean =
        template.isSystem && template.coverUri == legacyUri

    companion object {
        fun byId(id: Long?): BuiltInCardFace? = entries.firstOrNull { it.id == id }

        fun forTemplate(template: CardTemplate): BuiltInCardFace? =
            if (template.isSystem && !template.isDeleted) {
                entries.firstOrNull { template.coverUri == it.coverUri || it.isLegacyCover(template) }
            } else null
    }
}

internal fun List<CardTemplate>.selectableCardTemplates(): List<CardTemplate> =
    filterNot { it.isDeleted || BuiltInCardFace.forTemplate(it)?.isLegacyDefault(it) == true }

internal fun CardTemplate?.resolveBuiltInAppearance(templates: List<CardTemplate>): CardTemplate? {
    val preset = if (this == null) BuiltInCardFace.IridescentLight else BuiltInCardFace.forTemplate(this)
        ?.takeIf { it.isLegacyDefault(this) }
    return preset?.let { face -> templates.firstOrNull { it.id == face.id && !it.isDeleted } } ?: this
}
