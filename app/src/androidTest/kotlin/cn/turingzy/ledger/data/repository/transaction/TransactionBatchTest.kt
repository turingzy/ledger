package cn.turingzy.ledger.data.repository.transaction

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import cn.turingzy.ledger.data.database.LedgerDatabase
import cn.turingzy.ledger.data.database.entity.AccountEntity
import cn.turingzy.ledger.data.database.entity.CategoryEntity
import cn.turingzy.ledger.data.database.entity.ExchangeRateEntity
import cn.turingzy.ledger.data.database.entity.IconEntity
import cn.turingzy.ledger.data.database.entity.TransactionEntity
import cn.turingzy.ledger.data.repository.currency.ExchangeRateRepository
import cn.turingzy.ledger.data.repository.currency.ExchangeRateSource
import cn.turingzy.ledger.data.repository.currency.MoneyValuation
import cn.turingzy.ledger.data.repository.settings.AppPreferencesRepository
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.transaction.TransactionBatchChange
import cn.turingzy.ledger.model.transaction.TransactionType
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionBatchTest {
    @Test
    fun invalidAccountMoveRollsBackAndValidChangesUpdateBalancesExactlyOnce() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build()
        try {
            val accounts = database.accountDao()
            val transactions = database.transactionDao()
            val rates = ExchangeRateRepository(database, object : ExchangeRateSource {
                override suspend fun fetch(start: LocalDate, end: LocalDate, currencies: Set<String>): List<ExchangeRateEntity> =
                    error("Batch updates must reuse the existing valuation")
            })
            val repository = DefaultTransactionRepository(
                database, transactions, accounts, AppPreferencesRepository(context),
                MoneyValuation(database, rates), rates, Dispatchers.IO,
            )
            val source = accounts.insertAccount(AccountEntity(name = "Source", type = AccountType.CASH, currentBalance = 800L))
            val target = accounts.insertAccount(AccountEntity(name = "Target", type = AccountType.CASH, currentBalance = 1_000L))
            val dollar = accounts.insertAccount(AccountEntity(name = "Dollars", type = AccountType.CASH, currency = "USD", currentBalance = 900L))
            val icon = database.iconDao().insertIcon(IconEntity(keyName = "batch", displayName = "Batch", iconUri = "res://"))
            val firstCategory = database.categoryDao().insertCategory(CategoryEntity(name = "First", type = CategoryType.EXPENSE, iconId = icon))
            val secondCategory = database.categoryDao().insertCategory(CategoryEntity(name = "Second", type = CategoryType.EXPENSE, iconId = icon))
            val expense = transactions.insertTransaction(TransactionEntity(
                type = TransactionType.EXPENSE, amount = 200L, accountId = source, categoryId = firstCategory,
                transactionTime = 1L, baseAmount = 200L, baseCurrency = "CNY", importKey = "imported",
            ))
            val foreignExpense = transactions.insertTransaction(TransactionEntity(
                type = TransactionType.EXPENSE, amount = 100L, accountId = dollar, categoryId = firstCategory,
                transactionTime = 2L,
            ))

            val failed = runCatching {
                repository.applyBatch(linkedSetOf(expense, foreignExpense), TransactionBatchChange.Account(target))
            }
            assertTrue(failed.exceptionOrNull() is IllegalArgumentException)
            assertEquals(source, transactions.getTransactionByIdOnce(expense)?.accountId)
            assertEquals(800L, accounts.getAccountByIdOnce(source)?.currentBalance)
            assertEquals(1_000L, accounts.getAccountByIdOnce(target)?.currentBalance)

            repository.applyBatch(setOf(expense), TransactionBatchChange.Account(target))
            repository.applyBatch(setOf(expense, foreignExpense), TransactionBatchChange.Category(secondCategory))
            val moved = requireNotNull(transactions.getTransactionByIdOnce(expense))
            assertEquals(target, moved.accountId)
            assertEquals(secondCategory, moved.categoryId)
            assertEquals(200L, moved.baseAmount)
            assertEquals("imported", moved.importKey)
            assertEquals(1_000L, accounts.getAccountByIdOnce(source)?.currentBalance)
            assertEquals(800L, accounts.getAccountByIdOnce(target)?.currentBalance)

            repository.applyBatch(setOf(expense, foreignExpense), TransactionBatchChange.Delete)
            assertTrue(requireNotNull(transactions.getTransactionByIdOnce(expense)).isDeleted)
            assertTrue(requireNotNull(transactions.getTransactionByIdOnce(foreignExpense)).isDeleted)
            assertEquals(1_000L, accounts.getAccountByIdOnce(target)?.currentBalance)
            assertEquals(1_000L, accounts.getAccountByIdOnce(dollar)?.currentBalance)
        } finally {
            database.close()
        }
    }
}
