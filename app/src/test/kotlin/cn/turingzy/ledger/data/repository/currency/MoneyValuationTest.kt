package cn.turingzy.ledger.data.repository.currency

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.AccountWithTemplate
import cn.turingzy.ledger.model.transaction.CurrencySnapshot
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.data.database.LedgerDatabase
import io.mockk.*
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MoneyValuationTest {
    private val database: LedgerDatabase = mockk(relaxed = true)
    private val rates: ExchangeRateRepository = mockk()
    private val valuation = MoneyValuation(database, rates)
    private val usd = Account(1, "美元", AccountType.CASH, currency = "USD")
    private val cny = Account(2, "人民币", AccountType.CASH)
    private val transaction = Transaction(type = TransactionType.EXPENSE, amount = 10000L, accountId = 1, categoryId = 1,
        transactionTime = Instant.parse("2026-01-02T12:00:00Z").toEpochMilli())

    @Test fun recordsOriginalAndPrimaryAmountsWithoutChangingTheOriginal() = runTest {
        coEvery { rates.quote("USD", "CNY", any()) } returns ExchangeQuote("7.2", "2026-01-02")
        val valued = valuation.transaction(transaction, usd, null, "CNY")
        assertEquals(10000L, valued.amount)
        assertEquals(72000L, valued.baseAmount)
        assertEquals("7.2", valued.baseRate)
        assertEquals("2026-01-02", valued.baseRateDate)
    }

    @Test fun persistedHistoricalSnapshotDoesNotChangeWithCurrentRates() = runTest {
        val saved = transaction.copy(baseAmount = 70000L, baseCurrency = "CNY", baseRate = "7", baseRateDate = "2026-01-02")
        assertEquals(saved.copy(currencySnapshots = listOf(CurrencySnapshot("CNY", 70000L, "7", "2026-01-02"))), valuation.transaction(saved, usd, null, "CNY"))
        coVerify(exactly = 0) { rates.quote(any(), any(), any()) }
    }

    @Test fun crossCurrencyTransferStoresBothSidesAndHonorsActualDestinationAmount() = runTest {
        coEvery { rates.quote("USD", "CNY", any()) } returns ExchangeQuote("7.2", "2026-01-02")
        val transfer = transaction.copy(type = TransactionType.TRANSFER, toAccountId = 2)
        val valued = valuation.transaction(transfer, usd, cny, "CNY")
        assertEquals(10000L, valued.amount)
        assertEquals(72000L, valued.toAmount)
        assertEquals("7.2", valued.transferRate)
        val custom = valuation.transaction(transfer.copy(toAmount = 71000L), usd, cny, "CNY")
        assertEquals(71000L, custom.toAmount)
        assertEquals(72000L, custom.baseAmount)
    }

    @Test fun changingPrimaryCurrencyRevaluesTheOriginalAmountRatherThanConvertedAmount() = runTest {
        val saved = transaction.copy(baseAmount = 72000L, baseCurrency = "CNY", baseRate = "7.2")
        coEvery { rates.quote("USD", "USD", any()) } returns ExchangeQuote("1", "2026-01-02")
        assertEquals(10000L, valuation.transaction(saved, usd, null, "USD").baseAmount)
    }

    @Test fun zeroBalanceNeedsNoInventedExchangeRate() = runTest {
        val valued = valuation.accounts(listOf(AccountWithTemplate(usd)), "CNY").single().account
        assertEquals(0L, valued.baseBalance)
        assertNull(valued.baseRate)
        coVerify(exactly = 0) { rates.quote(any(), any(), any()) }
    }
    @Test fun switchingPrimaryCurrencyBackRestoresTheOriginalSnapshot() = runTest {
        val original = transaction.copy(baseAmount = 70000L, baseCurrency = "CNY", baseRate = "7", baseRateDate = "2026-01-02")
        coEvery { rates.quote("USD", "USD", any()) } returns ExchangeQuote("1", "2026-01-02")
        val dollars = valuation.transaction(original, usd, null, "USD")
        coEvery { rates.quote("USD", "CNY", any()) } returns ExchangeQuote("8", "2026-01-02")
        val restored = valuation.transaction(dollars, usd, null, "CNY")
        assertEquals(70000L, restored.baseAmount)
        assertEquals("7", restored.baseRate)
        coVerify(exactly = 0) { rates.quote("USD", "CNY", any()) }
    }

}
