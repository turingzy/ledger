package cn.turingzy.ledger.model

data class LedgerCardAppearance(
    val backgroundFile: String? = null,
    val transparencyEnabled: Boolean = true,
    val transparencyPercent: Int = 50,
) {
    init {
        require(transparencyPercent in 0..100)
    }

    val overlayOpacity: Float
        get() = if (transparencyEnabled) (100 - transparencyPercent) / 100f else 1f
}
