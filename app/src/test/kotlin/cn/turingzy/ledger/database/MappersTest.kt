package cn.turingzy.ledger.database

import cn.turingzy.ledger.database.mapper.asExternalModel
import cn.turingzy.ledger.database.model.AccountEntity
import cn.turingzy.ledger.database.model.CardTemplateEntity
import cn.turingzy.ledger.database.model.CategoryEntity
import cn.turingzy.ledger.database.model.IconEntity
import cn.turingzy.ledger.database.model.PopulatedAccount
import cn.turingzy.ledger.database.model.PopulatedCategory
import cn.turingzy.ledger.database.model.PopulatedTransaction
import cn.turingzy.ledger.database.model.TransactionEntity
import cn.turingzy.ledger.model.AccountType
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {

    @Test
    fun transactionEntity_asExternalModel_mapsCorrectly() {
        val entity = TransactionEntity(
            transactionId = 42L,
            type = TransactionType.EXPENSE,
            amount = 3500L,
            accountId = 1L,
            toAccountId = null,
            categoryId = 2L,
            transactionTime = 1700000000000L,
            note = "Lunch",
            isDeleted = false,
        )

        val model = entity.asExternalModel()
        assertEquals(42L, model.id)
        assertEquals(TransactionType.EXPENSE, model.type)
        assertEquals(3500L, model.amount)
        assertEquals(1L, model.accountId)
        assertNull(model.toAccountId)
        assertEquals(2L, model.categoryId)
        assertEquals("Lunch", model.note)
    }

    @Test
    fun populatedTransaction_asExternalModel_mapsAllRelations() {
        val iconEntity = IconEntity(iconId = 1L, keyName = "ic_food", displayName = "Food", iconUri = "res://food")
        val categoryEntity = CategoryEntity(categoryId = 2L, name = "Dining", type = CategoryType.EXPENSE, iconId = 1L)
        val populatedCategory = PopulatedCategory(category = categoryEntity, icon = iconEntity)

        val accountEntity = AccountEntity(accountId = 1L, name = "Debit Card", type = AccountType.DEBIT)
        val txEntity = TransactionEntity(
            transactionId = 10L,
            type = TransactionType.EXPENSE,
            amount = 2000L,
            accountId = 1L,
            toAccountId = null,
            categoryId = 2L,
            transactionTime = 1700000000000L,
        )

        val populatedTx = PopulatedTransaction(
            transaction = txEntity,
            account = accountEntity,
            toAccount = null,
            categoryWithIcon = populatedCategory,
        )

        val detail = populatedTx.asExternalModel()
        assertEquals(10L, detail.transaction.id)
        assertEquals("Debit Card", detail.account.name)
        assertNull(detail.toAccount)
        assertEquals("Dining", detail.category.category.name)
        assertEquals("ic_food", detail.category.icon.keyName)
    }

    @Test
    fun populatedAccount_asExternalModel_mapsTemplate() {
        val template = CardTemplateEntity(templateId = 5L, name = "Black Gold", coverUri = "res://black", textColor = "#FFFFFF")
        val account = AccountEntity(accountId = 1L, name = "Bank Card", type = AccountType.DEBIT, templateId = 5L)

        val populated = PopulatedAccount(account = account, template = template)
        val model = populated.asExternalModel()

        assertEquals(1L, model.account.id)
        assertNotNull(model.template)
        assertEquals("Black Gold", model.template?.name)
    }
}

