package cn.turingzy.ledger.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cn.turingzy.ledger.database.converter.RoomTypeConverters
import cn.turingzy.ledger.database.dao.AccountDao
import cn.turingzy.ledger.database.dao.BackupConfigDao
import cn.turingzy.ledger.database.dao.CardTemplateDao
import cn.turingzy.ledger.database.dao.CategoryDao
import cn.turingzy.ledger.database.dao.IconDao
import cn.turingzy.ledger.database.dao.ParseRuleDao
import cn.turingzy.ledger.database.dao.TransactionDao
import cn.turingzy.ledger.database.model.AccountEntity
import cn.turingzy.ledger.database.model.BackupConfigEntity
import cn.turingzy.ledger.database.model.CardTemplateEntity
import cn.turingzy.ledger.database.model.CategoryEntity
import cn.turingzy.ledger.database.model.IconEntity
import cn.turingzy.ledger.database.model.ParseRuleEntity
import cn.turingzy.ledger.database.model.TransactionEntity

@Database(
    entities = [
        IconEntity::class,
        CardTemplateEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        ParseRuleEntity::class,
        BackupConfigEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(RoomTypeConverters::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun iconDao(): IconDao
    abstract fun cardTemplateDao(): CardTemplateDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun parseRuleDao(): ParseRuleDao
    abstract fun backupConfigDao(): BackupConfigDao
}

