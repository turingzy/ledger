package cn.turingzy.ledger.model.settings

/**
 * 概览卡片外观配置模型。
 *
 * 控制账本首页与钱包顶部汇总卡片的背景图显示及半透明遮罩视觉效果。
 *
 * @property backgroundFile 自定义背景图片的文件名或本地路径，为 null 表示使用默认背景
 * @property transparencyPercent 背景透明度百分比（有效范围 0..100，0 为完全不透明，100 为完全透明）
 */
data class LedgerCardAppearance(
    val backgroundFile: String? = null,
    val transparencyPercent: Int = 50,
) {
    init {
        require(transparencyPercent in 0..100)
    }

    /**
     * 遮罩层不透明度（范围 0f..1f）。
     *
     * 用于 Compose 绘制蒙层时的 alpha 通道值，与透明度百分比呈互补关系。
     */
    val overlayOpacity: Float
        get() = (100 - transparencyPercent) / 100f
}
