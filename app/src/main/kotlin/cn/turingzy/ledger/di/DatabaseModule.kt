package cn.turingzy.ledger.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.turingzy.ledger.data.database.migration.Migration1To2
import cn.turingzy.ledger.data.database.migration.Migration2To3
import cn.turingzy.ledger.data.database.migration.Migration3To4
import cn.turingzy.ledger.data.database.migration.Migration4To5
import cn.turingzy.ledger.data.database.migration.Migration5To6
import cn.turingzy.ledger.data.database.LedgerDatabase
import cn.turingzy.ledger.data.database.dao.AccountDao
import cn.turingzy.ledger.data.database.dao.BackupConfigDao
import cn.turingzy.ledger.data.database.dao.CardTemplateDao
import cn.turingzy.ledger.data.database.dao.CategoryDao
import cn.turingzy.ledger.data.database.dao.IconDao
import cn.turingzy.ledger.data.database.dao.ParseRuleDao
import cn.turingzy.ledger.data.database.dao.TransactionDao
import cn.turingzy.ledger.data.database.seed.DefaultDatabaseSeeder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Provider
import javax.inject.Singleton

/**
 * 数据库依赖注入模块。
 *
 * 负责构建并提供 Room 数据库单例 [LedgerDatabase]，注册版本迁移与初始化种子数据回调，
 * 并对外暴露各个实体的 DAO 接口。
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** 数据库文件名。 */
    private const val DATABASE_NAME = "ledger.db"

    /**
     * 构建并提供 [LedgerDatabase] 数据库单例。
     *
     * 注册数据库迁移和初次建库的异步种子数据回调。
     *
     * @param context 应用程序全局上下文。
     * @param ioDispatcher IO 密集型协程调度器。
     * @param databaseProvider 用于延迟获取数据库实例以供种子播种使用的 Provider。
     * @return 构建完成的 [LedgerDatabase] 实例。
     */
    @Provides
    @Singleton
    fun providesLedgerDatabase(
        @ApplicationContext context: Context,
        @Dispatcher(LedgerDispatchers.IO) ioDispatcher: CoroutineDispatcher,
        databaseProvider: Provider<LedgerDatabase>,
    ): LedgerDatabase {
        return Room.databaseBuilder(
            context,
            LedgerDatabase::class.java,
            DATABASE_NAME,
        ).addMigrations(Migration1To2, Migration2To3, Migration3To4, Migration4To5, Migration5To6).addCallback(
            object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    val database = databaseProvider.get()
                    val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
                    DefaultDatabaseSeeder.seedInitialData(database, scope)
                }
            },
        ).build()
    }

    /**
     * 提供图标数据访问对象 [IconDao]。
     */
    @Provides
    fun providesIconDao(database: LedgerDatabase): IconDao = database.iconDao()

    /**
     * 提供卡面模板数据访问对象 [CardTemplateDao]。
     */
    @Provides
    fun providesCardTemplateDao(database: LedgerDatabase): CardTemplateDao = database.cardTemplateDao()

    /**
     * 提供分类数据访问对象 [CategoryDao]。
     */
    @Provides
    fun providesCategoryDao(database: LedgerDatabase): CategoryDao = database.categoryDao()

    /**
     * 提供账户数据访问对象 [AccountDao]。
     */
    @Provides
    fun providesAccountDao(database: LedgerDatabase): AccountDao = database.accountDao()

    /**
     * 提供交易数据访问对象 [TransactionDao]。
     */
    @Provides
    fun providesTransactionDao(database: LedgerDatabase): TransactionDao = database.transactionDao()

    /**
     * 提供账单解析规则数据访问对象 [ParseRuleDao]。
     */
    @Provides
    fun providesParseRuleDao(database: LedgerDatabase): ParseRuleDao = database.parseRuleDao()

    /**
     * 提供备份配置数据访问对象 [BackupConfigDao]。
     */
    @Provides
    fun providesBackupConfigDao(database: LedgerDatabase): BackupConfigDao = database.backupConfigDao()
}

