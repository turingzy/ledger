package cn.turingzy.ledger.data.repository.currency

import cn.turingzy.ledger.data.database.LedgerDatabase
import cn.turingzy.ledger.data.database.dao.ExchangeRateDao
import cn.turingzy.ledger.data.database.entity.ExchangeRateEntity
import io.mockk.*
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ExchangeRateRepositoryTest {
    private val storage = mutableListOf<ExchangeRateEntity>()
    private val dao: ExchangeRateDao = mockk {
        coEvery { rows() } answers { storage.sortedBy { it.date }.toList() }
        coEvery { insert(any()) } answers {
            val rows = firstArg<List<ExchangeRateEntity>>()
            rows.forEach { row -> storage.removeAll { it.date == row.date && it.currency == row.currency }; storage += row }
        }
    }
    private val database: LedgerDatabase = mockk { every { exchangeRateDao() } returns dao }
    private val source: ExchangeRateSource = mockk()
    private val repository = ExchangeRateRepository(database, source)
    private val date = LocalDate.of(2026, 1, 2)

    @Test fun crossesRatesThroughUsdWithoutFloatingPointMoney() = runTest {
        storage += ExchangeRateEntity(date.toString(), "CNY", "7.2")
        storage += ExchangeRateEntity(date.toString(), "HKD", "7.8")
        assertEquals("7.2", repository.quote("USD", "CNY", date).rate)
        assertEquals("0.923076923076923077", repository.quote("HKD", "CNY", date).rate)
        assertEquals("1", repository.quote("CNY", "CNY", date).rate)
    }

    @Test fun offlineReadsUseLastAvailableQuoteAndKeepItsActualDate() = runTest {
        storage += ExchangeRateEntity(date.minusDays(10).toString(), "CNY", "7.1")
        coEvery { source.fetch(any(), any(), any()) } throws IOException("offline")
        val quote = repository.quote("USD", "CNY", date)
        assertEquals("7.1", quote.rate)
        assertEquals(date.minusDays(10).toString(), quote.date)
        repository.quote("USD", "CNY", date)
        coVerify(exactly = 1) { source.fetch(any(), any(), any()) }
    }

    @Test fun unavailableRatesFailExplicitlyInsteadOfUsingOneToOne() = runTest {
        coEvery { source.fetch(any(), any(), any()) } throws IOException("offline")
        val error = runCatching { repository.quote("USD", "CNY", date) }.exceptionOrNull()
        assertTrue(error is IllegalStateException)
        assertTrue(error?.message.orEmpty().contains("CNY"))
    }

    @Test fun manualQuotesAreNotOverwrittenByAutomaticRefresh() = runTest {
        repository.setManualRate("USD", "CNY", "7.3", date)
        coEvery { source.fetch(any(), any(), any()) } returns listOf(ExchangeRateEntity(date.toString(), "CNY", "7.1"))
        repository.prepare(setOf(date), setOf("CNY", "USD"), refresh = true)
        assertEquals("7.3", repository.quote("USD", "CNY", date).rate)
    }

    @Test fun manualQuoteToUsdCanBeUsedForOtherPairs() = runTest {
        repository.setManualRate("HKD", "USD", "0.125", date)
        storage += ExchangeRateEntity(date.toString(), "CNY", "7.2")
        assertEquals("0.9", repository.quote("HKD", "CNY", date).rate)
    }
    @Test fun manualRateSupportsARegionalPrimaryCurrencyOtherThanCnyOrUsd() = runTest {
        repository.setManualRate("USD", "EUR", "0.9", date)
        assertEquals("0.9", repository.quote("USD", "EUR", date).rate)
    }

}
