package cn.turingzy.ledger.ui.component.card.bank

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

internal val DefaultTitaniumCardTuning = TitaniumCardTuning()
