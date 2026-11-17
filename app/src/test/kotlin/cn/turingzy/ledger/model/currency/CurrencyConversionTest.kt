package cn.turingzy.ledger.model.currency

import org.junit.Assert.*
import org.junit.Test

class CurrencyConversionTest {
    @Test fun roundsOnlyTheFinalCentAndPreservesNegativeBalances() {
        assertEquals(720L, convertCents(100L, "7.2"))
        assertEquals(1L, convertCents(1L, "0.5"))
        assertEquals(-720L, convertCents(-100L, "7.2"))
        assertEquals(9007199254740993L, convertCents(9007199254740993L, "1"))
    }
    @Test fun rejectsInvalidRatesAndOverflow() {
        assertThrows(IllegalArgumentException::class.java) { convertCents(100L, "0") }
        assertThrows(IllegalArgumentException::class.java) { convertCents(100L, "-1") }
        assertThrows(IllegalArgumentException::class.java) { convertCents(100L, "1e1000000") }
        assertThrows(ArithmeticException::class.java) { convertCents(Long.MAX_VALUE, "2") }
    }
}
