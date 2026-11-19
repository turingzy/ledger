package cn.turingzy.ledger.ui.interaction

import androidx.compose.runtime.staticCompositionLocalOf
import cn.turingzy.ledger.ui.theme.LedgerInteractionStyle

internal val LocalUiInteractionClock = staticCompositionLocalOf<() -> Long> {
    { System.nanoTime() / 1_000_000L }
}

internal class ClickGate(
    private val intervalMillis: Long = LedgerInteractionStyle.clickDebounceMillis,
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private var lastAccepted: Long? = null

    init {
        require(intervalMillis >= 0L)
    }

    fun tryAccept(): Boolean {
        val now = clockMillis()
        val previous = lastAccepted
        if (previous != null && now >= previous && now - previous < intervalMillis) return false
        lastAccepted = now
        return true
    }
}
