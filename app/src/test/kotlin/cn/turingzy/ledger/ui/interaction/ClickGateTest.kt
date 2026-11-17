package cn.turingzy.ledger.ui.interaction

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClickGateTest {
    @Test
    fun firstTapIsImmediateAndTheBoundaryAcceptsTheNextTap() {
        var now = 0L
        val gate = ClickGate(intervalMillis = 350L, clockMillis = { now })
        assertTrue(gate.tryAccept())
        assertFalse(gate.tryAccept())
        now = 349L
        assertFalse(gate.tryAccept())
        now = 350L
        assertTrue(gate.tryAccept())
    }

    @Test
    fun rejectedTapsDoNotExtendTheSuppressionWindow() {
        var now = 0L
        val gate = ClickGate(clockMillis = { now })
        assertTrue(gate.tryAccept())
        now = 300L
        assertFalse(gate.tryAccept())
        now = 351L
        assertTrue(gate.tryAccept())
    }

    @Test
    fun unrelatedControlsHaveIndependentGates() {
        val first = ClickGate(clockMillis = { 0L })
        val second = ClickGate(clockMillis = { 0L })
        assertTrue(first.tryAccept())
        assertTrue(second.tryAccept())
        assertFalse(first.tryAccept())
    }

    @Test
    fun aResetClockCannotLockOutTheControl() {
        var now = 500L
        val gate = ClickGate(clockMillis = { now })
        assertTrue(gate.tryAccept())
        now = 0L
        assertTrue(gate.tryAccept())
        assertFalse(gate.tryAccept())
    }
}
