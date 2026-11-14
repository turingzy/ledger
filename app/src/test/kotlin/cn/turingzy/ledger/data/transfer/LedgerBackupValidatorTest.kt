package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class LedgerBackupValidatorTest {
    private val bills = LedgerBillSnapshot(
        accounts = listOf(Account(1, "现金", AccountType.CASH, initialBalance = 10000, currentBalance = 9999)),
        transactions = listOf(Transaction(1, TransactionType.EXPENSE, 1, 1, categoryId = 1, transactionTime = 1000)),
        categories = listOf(Category(1, "餐饮", CategoryType.EXPENSE, 1)),
        icons = listOf(Icon(1, "food", "餐饮", "res://food")), templates = emptyList(),
    )
    @Test fun backupRoundTripPreservesCentsAndTombstones() {
        val snapshot = LedgerBackup(bills = bills.copy(accounts = bills.accounts.map { it.copy(isDeleted = true) }))
        val decoded = Json.decodeFromString<LedgerBackup>(Json.encodeToString(snapshot))
        LedgerBackupValidator.validate(decoded)
        assertEquals(snapshot, decoded)
    }
    @Test fun manuallyAdjustedBalancesArePreserved() {
        val snapshot = LedgerBackup(bills = bills.copy(accounts = bills.accounts.map { it.copy(currentBalance = 12000) }))
        LedgerBackupValidator.validate(snapshot)
        assertEquals(12000L, Json.decodeFromString<LedgerBackup>(Json.encodeToString(snapshot)).bills?.accounts?.single()?.currentBalance)
    }
    @Test fun debugAccountIdsAreValidBackupReferences() {
        LedgerBackupValidator.validate(LedgerBackup(bills = bills.copy(
            accounts = bills.accounts.map { it.copy(id = -10001) },
            transactions = bills.transactions.map { it.copy(accountId = -10001) },
        )))
    }
    @Test fun invalidReferencesAndDuplicateIdsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { LedgerBackupValidator.validate(LedgerBackup(bills = bills.copy(icons = emptyList()))) }
        assertThrows(IllegalArgumentException::class.java) { LedgerBackupValidator.validate(LedgerBackup(bills = bills.copy(accounts = bills.accounts + bills.accounts))) }
    }
    @Test fun unsupportedAndEmptyBackupsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { LedgerBackupValidator.validate(LedgerBackup(version = 2, bills = bills)) }
        assertThrows(IllegalArgumentException::class.java) { LedgerBackupValidator.validate(LedgerBackup()) }
    }
    @Test fun deletedTransactionsDoNotChangeBalance() {
        LedgerBackupValidator.validate(LedgerBackup(bills = bills.copy(
            accounts = bills.accounts.map { it.copy(currentBalance = 10000) },
            transactions = bills.transactions.map { it.copy(isDeleted = true) },
        )))
    }
}
