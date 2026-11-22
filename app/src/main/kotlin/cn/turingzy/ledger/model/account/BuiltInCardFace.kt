package cn.turingzy.ledger.model.account

/**
 * 系统内置卡片卡面预设枚举。
 *
 * 管理内置卡面的 ID、名称、资源 URI 以及历史版本迁移兼容逻辑。
 *
 * @property id 预设唯一标识（负数，避免与用户自定义 ID 冲突）
 * @property nameZh 预设中文名称
 * @property coverUri 预设封面资源的 URI
 * @property legacyName 旧版本中对应的预设名称（用于兼容迁移）
 * @property legacyUri 旧版本中对应的封面资源 URI
 * @property legacyInk 旧版本中使用的文字颜色十六进制代码
 */
internal enum class BuiltInCardFace(
    val id: Long,
    private val nameZh: String,
    private val coverUri: String,
    private val legacyName: String? = null,
    private val legacyUri: String? = null,
    private val legacyInk: String = "#FFFFFF",
) {
    /**
     * 流光钛金预设（浅色金属质感）。
     */
    IridescentLight(-104L, "流光钛金", "res://card-face/iridescent"),

    /**
     * 暮光钛金预设（深色沉稳质感，兼容旧版“商务黑金”）。
     */
    IridescentDark(-103L, "暮光钛金", "res://card-face/twilight", "商务黑金", "res://drawable/card_black_gold", "#D4AF37"),

    /**
     * 静谧珠光预设（浅色珠光质感，兼容旧版“活力薄荷”）。
     */
    PearlLight(-102L, "静谧珠光", "res://card-face/pearl", "活力薄荷", "res://drawable/card_mint_green"),

    /**
     * 深海珠光预设（深邃质感，兼容旧版“经典深蓝”）。
     */
    PearlDark(-101L, "深海珠光", "res://card-face/deep-sea", "经典深蓝", "res://drawable/card_classic_blue");

    /**
     * 将当前内置卡面转换为 [CardTemplate] 实例。
     *
     * @param appearance 可选的卡片模板实例，用于覆盖默认名称、自定义封面或状态
     * @return 转换后的 [CardTemplate]
     */
    fun asTemplate(appearance: CardTemplate? = null) = CardTemplate(
        id = id,
        name = appearance?.name?.takeIf { it.isNotBlank() } ?: nameZh,
        coverUri = appearance?.coverUri?.takeIf { it.isNotBlank() && !it.startsWith("res://") } ?: coverUri,
        textColor = appearance?.textColor ?: "#FFFFFF",
        isSystem = true,
        isDeleted = appearance?.isDeleted ?: false,
    )

    /**
     * 检查给定的卡片模板是否使用了当前预设的默认名称。
     *
     * @param template 待检查的卡片模板
     * @return true 表示使用了默认名称或符合旧版默认规则
     */
    fun hasDefaultName(template: CardTemplate): Boolean =
        if (template.id == id) template.name == nameZh else isLegacyDefault(template)

    /**
     * 检查给定的卡片模板是否完全符合旧版默认预设属性（封面、名称及文字颜色一致）。
     *
     * @param template 待检查的卡片模板
     * @return true 表示为旧版默认模板
     */
    fun isLegacyDefault(template: CardTemplate): Boolean =
        isLegacyCover(template) && template.name == legacyName && usesLegacyInk(template)

    /**
     * 检查给定的卡片模板是否使用了旧版预设的默认文字颜色。
     *
     * @param template 待检查的卡片模板
     * @return true 表示使用的是旧版文字颜色
     */
    fun usesLegacyInk(template: CardTemplate): Boolean =
        isLegacyCover(template) && template.textColor.equals(legacyInk, ignoreCase = true)

    private fun isLegacyCover(template: CardTemplate): Boolean =
        template.isSystem && template.coverUri == legacyUri

    companion object {
        /**
         * 根据预设 ID 查找对应的内置卡面枚举项。
         *
         * @param id 预设 ID
         * @return 匹配的 [BuiltInCardFace]，未找到则返回 null
         */
        fun byId(id: Long?): BuiltInCardFace? = entries.firstOrNull { it.id == id }

        /**
         * 根据卡片模板匹配对应的内置卡面预设。
         *
         * 优先根据 ID 匹配，其次通过封面 URI 或旧版资源 URI 匹配。
         *
         * @param template 卡片模板
         * @return 匹配的 [BuiltInCardFace]，若非系统模板或已删除则返回 null
         */
        fun forTemplate(template: CardTemplate): BuiltInCardFace? =
            if (template.isSystem && !template.isDeleted) {
                byId(template.id) ?: entries.firstOrNull { template.coverUri == it.coverUri || it.isLegacyCover(template) }
            } else null
    }
}

/**
 * 过滤出供用户选择的有效卡片模板列表。
 *
 * 排除已软删除的模板以及旧版默认过渡模板。
 *
 * @return 过滤后的卡片模板列表
 */
internal fun List<CardTemplate>.selectableCardTemplates(): List<CardTemplate> =
    filterNot { it.isDeleted || BuiltInCardFace.forTemplate(it)?.isLegacyDefault(it) == true }

/**
 * 解析并映射卡片模板的内置外观。
 *
 * 若模板为 null 或属于旧版默认预设，则将其解析为最新的内置卡面预设；否则保留原模板。
 *
 * @param templates 当前系统已加载的卡片模板列表
 * @return 解析后的卡片模板；若未匹配到内置模板则返回自身
 */
internal fun CardTemplate?.resolveBuiltInAppearance(templates: List<CardTemplate>): CardTemplate? {
    val preset = if (this == null) BuiltInCardFace.IridescentLight else BuiltInCardFace.forTemplate(this)
        ?.takeIf { it.isLegacyDefault(this) }
    return preset?.let { face -> templates.firstOrNull { it.id == face.id && !it.isDeleted } } ?: this
}
