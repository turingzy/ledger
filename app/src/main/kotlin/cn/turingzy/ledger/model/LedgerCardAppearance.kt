package cn.turingzy.ledger.model

data class LedgerCardAppearance(
    val backgroundFile: String? = null,
    val transparencyPercent: Int = 50,
) {
    init {
        require(transparencyPercent in 0..100)
    }

    val overlayOpacity: Float
        get() = (100 - transparencyPercent) / 100f
}
