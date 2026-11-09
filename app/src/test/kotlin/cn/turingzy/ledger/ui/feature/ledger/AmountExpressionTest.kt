package cn.turingzy.ledger.ui.feature.ledger

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountExpressionTest {
    @Test
    fun calculatesUsingExactCents() {
        assertEquals("0.3", evaluateMathExpression("0.1+0.2"))
        assertEquals("8", evaluateMathExpression("10.50-2.50"))
        assertEquals("-1.25", evaluateMathExpression("1-2.25"))
        assertEquals("1.75", evaluateMathExpression("-1.25+3"))
        assertEquals("10", evaluateMathExpression("10+"))
    }

    @Test
    fun rejectsInvalidAmountsAndOverflow() {
        assertEquals("", evaluateMathExpression("1.234"))
        assertEquals("", evaluateMathExpression("1x2"))
        assertEquals("", evaluateMathExpression("92233720368547758.08"))
        assertEquals("", evaluateMathExpression("92233720368547758.07+0.01"))
        assertEquals("", evaluateMathExpression("-92233720368547758.07-0.01"))
        assertEquals("92233720368547758.07", evaluateMathExpression("92233720368547758.07"))
    }
}
