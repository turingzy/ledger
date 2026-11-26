package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.model.account.Account
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.account.BankCardDetails
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.model.backup.LedgerBackup
import cn.turingzy.ledger.model.backup.LedgerBillSnapshot
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionType
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
    @Test fun backupKeepsOnlyTheDisplayTailForProtectedCardDetails() {
        val card = bills.accounts.single().copy(
            type = AccountType.DEBIT,
            cardNumber = "1111",
            cardDetails = BankCardDetails("4111111111111111", "08/29", "123"),
        )
        val encoded = Json.encodeToString(LedgerBackup(bills = bills.copy(accounts = listOf(card))))
        val reopened = Json.decodeFromString<LedgerBackup>(encoded).bills?.accounts?.single()
        assertEquals("1111", reopened?.cardNumber)
        assertNull(reopened?.cardDetails)
        assertFalse(encoded.contains("4111111111111111"))
    }
    @Test fun legacyCardAppearanceRestoresImageAndDropsTransparency() {
        val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
        val backup = json.decodeFromString<LedgerBackup>(
            """{"settings":{"rules":[],"templates":[],"appearances":[{"target":"WALLET","transparency":65,"image":"YWJj"}]}}""",
        )
        LedgerBackupValidator.validate(backup)
        val appearance = requireNotNull(backup.settings).appearances.single()
        assertEquals(SummaryCardTarget.WALLET, appearance.target)
        assertEquals("YWJj", appearance.image)
        assertFalse(json.encodeToString(backup).contains("transparency"))
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
