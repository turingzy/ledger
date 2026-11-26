package cn.turingzy.ledger.model.settings

/**
 * 概览卡片外观配置模型。
 *
 * 记录账本与钱包汇总卡片使用的背景图片。
 *
 * @property backgroundFile 自定义背景图片的文件名或本地路径，为 null 表示使用默认背景
 */
data class LedgerCardAppearance(
    val backgroundFile: String? = null,
)
