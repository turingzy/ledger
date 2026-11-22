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

/**
 * Ledger 应用的核心 Room 数据库类。
 *
 * 维护系统图标、卡片模板、分类、账户、交易记录、解析规则、备份配置及汇率等数据表。
 */
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
    version = 5,
    exportSchema = false,
)
@TypeConverters(RoomTypeConverters::class)
abstract class LedgerDatabase : RoomDatabase() {
    /** 获取汇率数据访问对象 */
    abstract fun exchangeRateDao(): cn.turingzy.ledger.data.database.dao.ExchangeRateDao

    /** 获取图标数据访问对象 */
    abstract fun iconDao(): IconDao

    /** 获取卡片模板数据访问对象 */
    abstract fun cardTemplateDao(): CardTemplateDao

    /** 获取分类数据访问对象 */
    abstract fun categoryDao(): CategoryDao

    /** 获取账户数据访问对象 */
    abstract fun accountDao(): AccountDao

    /** 获取交易记录数据访问对象 */
    abstract fun transactionDao(): TransactionDao

    /** 获取账单解析规则数据访问对象 */
    abstract fun parseRuleDao(): ParseRuleDao

    /** 获取备份配置数据访问对象 */
    abstract fun backupConfigDao(): BackupConfigDao
}
