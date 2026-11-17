package cn.turingzy.ledger.data.debug

import androidx.room.withTransaction
import cn.turingzy.ledger.data.database.LedgerDatabase
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.data.database.entity.AccountEntity
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.transaction.TransactionType
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
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
                seedScenarios()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                Timber.e(failure, "Unable to seed development test data")
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
    private suspend fun seedScenarios() {
        val categories = database.categoryDao().getPopulatedCategories().first { items ->
            CategoryType.entries.all { type -> items.any { it.category.type == type } }
        }
        val data = debugScenarioData(
            Instant.now(), ZoneId.systemDefault(),
            categories.filter { it.category.type == CategoryType.EXPENSE }.map { it.category.categoryId },
            categories.filter { it.category.type == CategoryType.INCOME }.map { it.category.categoryId },
            categories.first { it.category.type == CategoryType.TRANSFER }.category.categoryId,
        )
        database.withTransaction {
            // The account survives soft deletion and prevents re-adding data after user edits.
            val exists = database.openHelper.writableDatabase.query(
                "SELECT 1 FROM tb_account WHERE account_id = ? LIMIT 1", arrayOf<Any>(data.accounts.first().id),
            ).use { it.moveToFirst() }
            if (exists) return@withTransaction
            val templateIds = database.cardTemplateDao().backupRows().filterNot { it.isDeleted }.map { it.templateId }.toSet()
            data.accounts.forEach { account ->
                database.accountDao().insertAccount(account.copy(templateId = account.templateId?.takeIf { it in templateIds }).asEntity())
            }
            data.transactions.forEach { database.transactionDao().insertTransaction(it.asEntity()) }
        }
    }

}
