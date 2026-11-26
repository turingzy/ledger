package cn.turingzy.ledger.data.database.seed

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import cn.turingzy.ledger.data.database.LedgerDatabase
import cn.turingzy.ledger.data.database.entity.CategoryEntity
import cn.turingzy.ledger.data.database.entity.IconEntity
import cn.turingzy.ledger.data.database.migration.Migration5To6
import cn.turingzy.ledger.data.database.migration.Migration6To7
import cn.turingzy.ledger.model.category.CategoryType
import java.util.UUID
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** Verifies both a freshly seeded ledger and an upgrade with existing category conflicts. */
class ExpandedSystemCategoriesTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun freshLedgerHasDistinctIconsAcrossAllBuiltInCategories() = runBlocking {
        val database = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java).build()
        try {
            coroutineScope { DefaultDatabaseSeeder.seedInitialData(database, this) }

            val categories = database.categoryDao().backupRows()
            assertEquals(23, categories.count { it.type == CategoryType.EXPENSE })
            assertEquals(19, categories.count { it.type == CategoryType.INCOME })
            assertEquals(1, categories.count { it.type == CategoryType.TRANSFER })
            assertEquals(categories.size, categories.map { it.iconId }.distinct().size)
        } finally {
            database.close()
        }
    }

    @Test
    fun upgradeAddsCategoriesWithoutReplacingCustomOrDeletedEntries() = runBlocking {
        val name = "category-upgrade-${UUID.randomUUID()}.db"
        val legacyIcon = IconEntity(iconId = 10, keyName = "ic_other", displayName = "其他", iconUri = "res://")
        val userIcon = IconEntity(iconId = 11, keyName = "user_icon", displayName = "自定义", iconUri = "res://")
        val custom = CategoryEntity(12, "买菜", CategoryType.EXPENSE, userIcon.iconId, sortOrder = 81)
        val deleted = CategoryEntity(13, "宠物", CategoryType.EXPENSE, userIcon.iconId, isDeleted = true)
        val otherExpense = CategoryEntity(7, "其他支出", CategoryType.EXPENSE, legacyIcon.iconId, isSystem = true)
        val otherIncome = CategoryEntity(10, "其他收入", CategoryType.INCOME, legacyIcon.iconId, isSystem = true)

        try {
            val legacy = Room.databaseBuilder(context, LedgerDatabase::class.java, name).build()
            try {
                legacy.iconDao().insertIcons(listOf(legacyIcon, userIcon))
                legacy.categoryDao().insertCategories(listOf(custom, deleted, otherExpense, otherIncome))
                val db = legacy.openHelper.writableDatabase
                // Rebuild the account table without the v7 column to exercise both migrations.
                val createAccount = db.query("SELECT sql FROM sqlite_master WHERE name = 'tb_account'").use {
                    check(it.moveToFirst())
                    it.getString(0).replace(", `encrypted_card_details` TEXT", "")
                }
                db.execSQL("DROP TABLE tb_account")
                db.execSQL(createAccount)
                db.execSQL("CREATE INDEX index_tb_account_template_id ON tb_account(template_id)")
                db.execSQL("CREATE INDEX idx_account_sort ON tb_account(sort_order, is_deleted)")
                db.execSQL("INSERT INTO tb_account (account_id, name, type, card_number) VALUES (1, 'Legacy card', 'DEBIT', '1234')")
                db.version = 5
            } finally {
                legacy.close()
            }

            val migrated = Room.databaseBuilder(context, LedgerDatabase::class.java, name)
                .addMigrations(Migration5To6, Migration6To7).build()
            try {
                val categories = migrated.categoryDao().backupRows()
                assertEquals(17, categories.count { it.type == CategoryType.EXPENSE })
                assertEquals(17, categories.count { it.type == CategoryType.INCOME })
                assertEquals(custom, categories.single { it.name == custom.name })
                assertEquals(deleted, categories.single { it.name == deleted.name })
                assertEquals(otherExpense, categories.single { it.categoryId == otherExpense.categoryId })
                val updatedIncome = categories.single { it.categoryId == otherIncome.categoryId }
                assertNotEquals(legacyIcon.iconId, updatedIncome.iconId)
                assertEquals(otherIncome, updatedIncome.copy(iconId = legacyIcon.iconId))
                assertEquals(userIcon, migrated.iconDao().getIconByIdOnce(userIcon.iconId))
                val account = requireNotNull(migrated.accountDao().getAccountByIdOnce(1L))
                assertEquals("1234", account.cardNumber)
                assertEquals(null, account.encryptedCardDetails)
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
