package cn.turingzy.ledger.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cn.turingzy.ledger.data.database.converter.RoomTypeConverters
import cn.turingzy.ledger.data.database.dao.AccountDao
import cn.turingzy.ledger.data.database.dao.BackupConfigDao
import cn.turingzy.ledger.data.database.dao.CardTemplateDao
import cn.turingzy.ledger.data.database.dao.CategoryDao
import cn.turingzy.ledger.data.database.dao.IconDao
import cn.turingzy.ledger.data.database.dao.ParseRuleDao
import cn.turingzy.ledger.data.database.dao.TransactionDao
import cn.turingzy.ledger.data.database.entity.AccountEntity
import cn.turingzy.ledger.data.database.entity.BackupConfigEntity
import cn.turingzy.ledger.data.database.entity.CardTemplateEntity
import cn.turingzy.ledger.data.database.entity.CategoryEntity
import cn.turingzy.ledger.data.database.entity.IconEntity
import cn.turingzy.ledger.data.database.entity.ParseRuleEntity
import cn.turingzy.ledger.data.database.entity.TransactionEntity

@Database(
    entities = [
        IconEntity::class,
        CardTemplateEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        ParseRuleEntity::class,
        BackupConfigEntity::class,
        cn.turingzy.ledger.data.database.entity.ExchangeRateEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
@TypeConverters(RoomTypeConverters::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun exchangeRateDao(): cn.turingzy.ledger.data.database.dao.ExchangeRateDao
    abstract fun iconDao(): IconDao
    abstract fun cardTemplateDao(): CardTemplateDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun parseRuleDao(): ParseRuleDao
    abstract fun backupConfigDao(): BackupConfigDao
}
