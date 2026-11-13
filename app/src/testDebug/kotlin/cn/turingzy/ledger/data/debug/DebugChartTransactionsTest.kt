package cn.turingzy.ledger.data.debug

import cn.turingzy.ledger.model.TransactionType
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DebugChartTransactionsTest {
    private val now = Instant.parse("2026-11-14T01:00:00Z")
    private val zone = ZoneId.of("Asia/Shanghai")
    private fun generate() = debugChartTransactions(now, zone, listOf(-10001L, -10002L), (1L..7L).toList(), listOf(8L, 9L, 10L))

    @Test fun generatesExactlyOneHundredRecognizableEntriesAcrossBothTypesAndAccounts() {
        val entries = generate()
        assertEquals(100, entries.size)
        assertEquals(100, entries.map { it.note }.distinct().size)
        assertEquals(20, entries.count { it.type == TransactionType.INCOME })
        assertEquals(80, entries.count { it.type == TransactionType.EXPENSE })
        assertEquals(setOf(-10001L, -10002L), entries.map { it.accountId }.toSet())
        assertTrue(entries.all { it.amount > 0L && it.note?.startsWith("[图表测试]") == true })
    }
    @Test fun coversRecentAndHistoricalChartsWithoutFutureDatesOrInvalidCategories() {
        val entries = generate()
        val today = now.atZone(zone).toLocalDate()
        val dates = entries.map { Instant.ofEpochMilli(it.transactionTime).atZone(zone).toLocalDate() }
        assertTrue(entries.all { it.transactionTime < now.toEpochMilli() })
        assertTrue(dates.count { it >= today.minusDays(6) } >= 28)
        assertTrue(dates.count { it >= today.withDayOfMonth(1) } >= 60)
        assertTrue(dates.min() < today.minusYears(1))
        assertEquals(7, entries.filter { it.type == TransactionType.EXPENSE }.map { it.categoryId }.distinct().size)
        assertEquals(3, entries.filter { it.type == TransactionType.INCOME }.map { it.categoryId }.distinct().size)
        assertEquals(generate(), entries)
    }
}
