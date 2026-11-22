package cn.turingzy.ledger.ui.interaction

import androidx.compose.runtime.staticCompositionLocalOf
import cn.turingzy.ledger.ui.theme.LedgerInteractionStyle

/**
 * 提供 UI 交互时间戳（毫秒）的时钟提供者 [CompositionLocal]，便于测试中模拟时间流逝。
 */
internal val LocalUiInteractionClock = staticCompositionLocalOf<() -> Long> {
    { System.nanoTime() / 1_000_000L }
}

/**
 * 点击防抖门限器。
 *
 * 用于防止短时间内重复点击触发多次事件（例如连击导致的重复提交或重复打开页面）。
 *
 * @param intervalMillis 两次有效点击之间的最小时间间隔（毫秒），默认为 [LedgerInteractionStyle.clickDebounceMillis]
 * @param clockMillis 获取当前时间戳（毫秒）的函数
 */
internal class ClickGate(
    private val intervalMillis: Long = LedgerInteractionStyle.clickDebounceMillis,
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private var lastAccepted: Long? = null

    init {
        require(intervalMillis >= 0L)
    }

    /**
     * 尝试接收一次点击事件。
     *
     * @return 如果距离上次接受点击的时间已超过设定的防抖间隔则返回 true，否则返回 false
     */
    fun tryAccept(): Boolean {
        val now = clockMillis()
        val previous = lastAccepted
        if (previous != null && now >= previous && now - previous < intervalMillis) return false
        lastAccepted = now
        return true
    }
}
