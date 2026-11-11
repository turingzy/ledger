package cn.turingzy.ledger.model

enum class AnalyticsCard(val storageKey: String) {
    COMPARISON("comparison"),
    TREND("trend"),
    CATEGORIES("categories"),
    AVERAGES("averages"),
}

data class AnalyticsCardSettings(
    val order: List<AnalyticsCard> = AnalyticsCard.entries.toList(),
    val disabled: Set<AnalyticsCard> = emptySet(),
) {
    val enabledCards: List<AnalyticsCard> get() = order.filter { it !in disabled }

    fun withEnabled(card: AnalyticsCard, enabled: Boolean): AnalyticsCardSettings =
        copy(disabled = if (enabled) disabled - card else disabled + card)

    fun withOrder(cards: List<AnalyticsCard>): AnalyticsCardSettings =
        copy(order = (cards + AnalyticsCard.entries).distinct())

    fun move(card: AnalyticsCard, index: Int): AnalyticsCardSettings {
        val previous = order.indexOf(card)
        if (previous < 0 || index !in order.indices || previous == index) return this
        return copy(order = order.toMutableList().apply { removeAt(previous); add(index, card) }.toList())
    }

    companion object {
        fun fromStored(order: List<String>, disabled: Set<String>): AnalyticsCardSettings {
            val cards = AnalyticsCard.entries.associateBy { it.storageKey }
            return AnalyticsCardSettings(
                order = (order.mapNotNull(cards::get) + AnalyticsCard.entries).distinct(),
                disabled = disabled.mapNotNull(cards::get).toSet(),
            )
        }
    }
}
