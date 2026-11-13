package cn.turingzy.ledger.data.debug

import androidx.room.withTransaction
import cn.turingzy.ledger.database.LedgerDatabase
import cn.turingzy.ledger.database.mapper.asEntity
import cn.turingzy.ledger.database.model.AccountEntity
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.TransactionType
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class DevelopmentDataInitializer @Inject constructor(
    private val database: LedgerDatabase,
    @Dispatcher(LedgerDispatchers.IO) dispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    fun initialize() {
        scope.launch {
            try {
                seed()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                Timber.e(failure, "Unable to seed chart test data")
            }
        }
    }

    private suspend fun seed() {
        val categories = database.categoryDao().getPopulatedCategories().first { items ->
            items.any { it.category.type == CategoryType.EXPENSE } && items.any { it.category.type == CategoryType.INCOME }
        }
        val accountIds = listOf(-10001L, -10002L)
        val entries = debugChartTransactions(
            Instant.now(), ZoneId.systemDefault(), accountIds,
            categories.filter { it.category.type == CategoryType.EXPENSE }.map { it.category.categoryId },
            categories.filter { it.category.type == CategoryType.INCOME }.map { it.category.categoryId },
        )
        database.withTransaction {
            // Deleted test accounts also mark completion, so user deletions are not recreated.
            val alreadySeeded = database.openHelper.writableDatabase.query(
                "SELECT 1 FROM tb_account WHERE account_id = ? LIMIT 1", arrayOf<Any>(accountIds.first()),
            ).use { it.moveToFirst() }
            if (alreadySeeded) return@withTransaction
            accountIds.forEachIndexed { index, id ->
                val balance = entries.filter { it.accountId == id }.sumOf {
                    if (it.type == TransactionType.INCOME) it.amount else -it.amount
                }
                database.accountDao().insertAccount(AccountEntity(
                    accountId = id,
                    name = if (index == 0) "图表测试·现金" else "图表测试·银行卡",
                    type = if (index == 0) AccountType.CASH else AccountType.DEBIT,
                    currentBalance = balance,
                ))
            }
            entries.forEach { database.transactionDao().insertTransaction(it.asEntity()) }
        }
    }
}
