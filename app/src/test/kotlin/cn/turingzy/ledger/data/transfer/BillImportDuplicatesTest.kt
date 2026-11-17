package cn.turingzy.ledger.data.transfer

import cn.turingzy.ledger.model.transaction.Transaction
import cn.turingzy.ledger.model.transaction.TransactionType
import org.junit.Assert.*
import org.junit.Test

class BillImportDuplicatesTest {
    private val coffee = Transaction(type = TransactionType.EXPENSE, amount = 1800L, accountId = 1L,
        categoryId = 1L, transactionTime = 1_700_000_000_000L, note = "咖啡")

    @Test fun repeatedTransactionIdsAreSkippedAcrossOverlappingFilesAndWithinAFile() {
        val existing = coffee.copy(importKey = "交易号:one")
        val next = coffee.copy(importKey = "交易号:two")
        val result = planBillImport(listOf(existing, next, next), listOf(existing), false)
        assertEquals(listOf(next), result.transactions)
        assertEquals(2, result.duplicates)
        assertEquals(0, result.possibleDuplicates)
    }

    @Test fun differentIdsKeepTwoRealPurchasesWithIdenticalDetails() {
        val existing = coffee.copy(importKey = "交易号:one")
        val next = coffee.copy(importKey = "交易号:two")
        assertEquals(listOf(next), planBillImport(listOf(next), listOf(existing), false).transactions)
    }

    @Test fun repeatedIdIsScopedToTheAccount() {
        val existing = coffee.copy(importKey = "交易号:one")
        val otherAccount = existing.copy(accountId = 2L)
        assertEquals(listOf(otherAccount), planBillImport(listOf(otherAccount), listOf(existing), false).transactions)
    }

    @Test fun identicalPurchasesInAFreshFileAreBothKept() {
        assertEquals(2, planBillImport(listOf(coffee, coffee), emptyList(), false).transactions.size)
    }

    @Test fun matchesAreCountedRatherThanCollapsingAllIdenticalPurchases() {
        val result = planBillImport(listOf(coffee, coffee), listOf(coffee), false)
        assertEquals(1, result.transactions.size)
        assertEquals(1, result.possibleDuplicates)
        assertEquals(2, planBillImport(listOf(coffee, coffee), listOf(coffee), true).transactions.size)
        assertEquals(0, planBillImport(listOf(coffee, coffee), listOf(coffee, coffee), false).transactions.size)
    }

    @Test fun changedCategoryDoesNotDefeatDuplicateDetectionButDifferentNotesDo() {
        assertEquals(1, planBillImport(listOf(coffee.copy(categoryId = 2L)), listOf(coffee), false).possibleDuplicates)
        assertEquals(1, planBillImport(listOf(coffee.copy(note = "午饭")), listOf(coffee), false).transactions.size)
    }

    @Test fun matchingAManualEntryRequiresConfirmationEvenWhenImportedRowHasAnId() {
        val imported = coffee.copy(importKey = "交易号:one")
        assertEquals(1, planBillImport(listOf(imported), listOf(coffee), false).possibleDuplicates)
        assertEquals(listOf(imported), planBillImport(listOf(imported), listOf(coffee), true).transactions)
    }

    @Test fun knownIdRemainsDuplicateAfterEditingAndDeletedEntriesCanBeImportedAgain() {
        val existing = coffee.copy(importKey = "交易号:one", note = "修改过的备注")
        assertEquals(1, planBillImport(listOf(coffee.copy(importKey = existing.importKey)), listOf(existing), true).duplicates)
        assertEquals(1, planBillImport(listOf(coffee), listOf(coffee.copy(isDeleted = true)), false).transactions.size)
    }
}
