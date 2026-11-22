package cn.turingzy.ledger.ui.component.card.bank

/**
 * 钛金属卡片物理倾斜与反光动效的调节参数。
 *
 * @property gravityAngleDegrees 设备重力陀螺仪驱动下的最大倾角（角度制）。
 * @property dragAngleDegrees 手势拖拽交互下的最大倾角（角度制）。
 * @property horizontalDragFraction 达到最大横向倾角所需的卡片宽度拖拽比例。
 * @property verticalDragFraction 达到最大纵向倾角所需的卡片高度拖拽比例。
 * @property shaderDragBandStrength 拖拽时 AGSL 着色器彩虹色带的增强强度。
 * @property shaderDragCoreStrength 拖拽时 AGSL 着色器高光核心的增强强度。
 * @property fallbackDragHighlightBoost 降级渲染路径下（Android 13 以下）拖拽高光的增益比例。
 */
internal data class TitaniumCardTuning(
    val gravityAngleDegrees: Float = 4.6f,
    val dragAngleDegrees: Float = 13f,
    // Fractions of the card width and height needed to reach full tilt.
    val horizontalDragFraction: Float = .23f,
    val verticalDragFraction: Float = .29f,
    val shaderDragBandStrength: Float = .16f,
    val shaderDragCoreStrength: Float = .18f,
    val fallbackDragHighlightBoost: Float = .60f,
)

/**
 * 默认钛金属卡片物理调节参数实例。
 */
internal val DefaultTitaniumCardTuning = TitaniumCardTuning()
