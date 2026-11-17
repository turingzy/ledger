package cn.turingzy.ledger.data.debug

import cn.turingzy.ledger.data.transfer.BillFileCodec
import cn.turingzy.ledger.data.transfer.BillImportPlanner
import cn.turingzy.ledger.data.transfer.planBillImport
import cn.turingzy.ledger.model.transfer.BillParseRule
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.transaction.TransactionType
import java.io.File
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class DebugScenarioDataTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val now = Instant.parse("2026-11-15T04:00:00Z")
    private val categories = listOf(
        Category(1, "餐饮", CategoryType.EXPENSE, 1), Category(2, "交通", CategoryType.EXPENSE, 1),
        Category(8, "工资", CategoryType.INCOME, 1), Category(11, "转账", CategoryType.TRANSFER, 1),
    )
    private fun generate() = debugScenarioData(now, zone, listOf(1L, 2L), listOf(8L), 11L)
    private fun fixture(name: String): List<cn.turingzy.ledger.model.transaction.Transaction> {
        val file = listOf(File("../test-data/import/$name"), File("test-data/import/$name")).first { it.exists() }
        return BillImportPlanner.plan(BillFileCodec.read(file.readBytes()), BillParseRule.defaults().first(), null,
            generate().accounts.filterNot { it.isDeleted }, categories)
    }

    @Test fun coversCurrenciesTypesEmptyAndExcludedAccountsWithoutFutureOrInvalidTransactions() {
        val data = generate()
        assertEquals(10, data.accounts.size)
        assertEquals(323, data.transactions.size)
        assertEquals(322, data.transactions.count { !it.isDeleted })
        assertEquals(setOf("CNY", "USD", "HKD", "EUR"), data.accounts.map { it.currency }.toSet())
        assertEquals(TransactionType.entries.toSet(), data.transactions.map { it.type }.toSet())
        assertTrue(data.transactions.all { it.amount > 0L && it.transactionTime < now.toEpochMilli() && it.note.orEmpty().startsWith("[场景测试]") })
        assertTrue(data.accounts.any { !it.includeInTotal && it.currentBalance > 0L })
        assertTrue(data.accounts.any { it.isDeleted })
        assertTrue(data.accounts.any { it.currency == "EUR" && data.transactions.none { entry -> entry.accountId == it.id } })
        assertTrue(data.accounts.any { it.type == cn.turingzy.ledger.model.account.AccountType.CREDIT && it.currentBalance < 0L })
        assertEquals(generate(), data)
        val months = data.transactions.map { YearMonth.from(Instant.ofEpochMilli(it.transactionTime).atZone(zone)) }.toSet()
        assertTrue(months.size >= 24)
    }

    @Test fun balancesIncludeBothSidesOfTransfersAndIgnoreDeletedTransactions() {
        val data = generate()
        data.accounts.forEach { account ->
            val incoming = data.transactions.filter { !it.isDeleted && (it.toAccountId == account.id || it.accountId == account.id && it.type == TransactionType.INCOME) }.sumOf { it.amount }
            val outgoing = data.transactions.filter { !it.isDeleted && it.accountId == account.id && it.type != TransactionType.INCOME }.sumOf { it.amount }
            assertEquals(account.initialBalance + incoming - outgoing, account.currentBalance)
        }
        val byId = data.accounts.associateBy { it.id }
        data.transactions.filter { it.type == TransactionType.TRANSFER }.forEach {
            assertNotEquals(it.accountId, it.toAccountId)
            assertEquals(byId.getValue(it.accountId).currency, byId.getValue(checkNotNull(it.toAccountId)).currency)
        }
    }

    @Test fun importFixturesMatchSeededKeysAndHandleOverlappingFiles() {
        val existing = generate().transactions
        val pending = fixture("01-known-ids.csv")
        val first = planBillImport(pending, existing, false)
        assertEquals(3, first.transactions.size)
        assertEquals(2, first.duplicates)
        assertEquals(0, first.possibleDuplicates)
        val repeated = planBillImport(pending, existing + first.transactions, false)
        assertEquals(0, repeated.transactions.size)
        assertEquals(5, repeated.duplicates)
        val overlapping = planBillImport(fixture("03-overlapping-file.csv"), existing + first.transactions, false)
        assertEquals(1, overlapping.transactions.size)
        assertEquals(1, overlapping.duplicates)
    }

    @Test fun noIdFixtureRequiresConfirmationAndKeepsTwoIdenticalNewPurchases() {
        val existing = generate().transactions
        val pending = fixture("02-without-ids.csv")
        val skipped = planBillImport(pending, existing, false)
        assertEquals(1, skipped.possibleDuplicates)
        assertEquals(3, skipped.transactions.size)
        assertEquals(4, planBillImport(pending, existing, true).transactions.size)
        assertEquals(4, planBillImport(pending, existing + skipped.transactions, false).possibleDuplicates)
    }

    @Test fun invalidCurrencyFixtureRejectsTheWholePlan() {
        assertThrows(IllegalArgumentException::class.java) { fixture("04-invalid-currency.csv") }
    }
}
