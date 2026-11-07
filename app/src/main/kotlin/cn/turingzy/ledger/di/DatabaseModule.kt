package cn.turingzy.ledger.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import cn.turingzy.ledger.database.LedgerDatabase
import cn.turingzy.ledger.database.dao.AccountDao
import cn.turingzy.ledger.database.dao.BackupConfigDao
import cn.turingzy.ledger.database.dao.CardTemplateDao
import cn.turingzy.ledger.database.dao.CategoryDao
import cn.turingzy.ledger.database.dao.IconDao
import cn.turingzy.ledger.database.dao.ParseRuleDao
import cn.turingzy.ledger.database.dao.TransactionDao
import cn.turingzy.ledger.database.util.DefaultDatabaseSeeder
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
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

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "ledger.db"

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
        ).addCallback(
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

    @Provides
    fun providesIconDao(database: LedgerDatabase): IconDao = database.iconDao()

    @Provides
    fun providesCardTemplateDao(database: LedgerDatabase): CardTemplateDao = database.cardTemplateDao()

    @Provides
    fun providesCategoryDao(database: LedgerDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun providesAccountDao(database: LedgerDatabase): AccountDao = database.accountDao()

    @Provides
    fun providesTransactionDao(database: LedgerDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun providesParseRuleDao(database: LedgerDatabase): ParseRuleDao = database.parseRuleDao()

    @Provides
    fun providesBackupConfigDao(database: LedgerDatabase): BackupConfigDao = database.backupConfigDao()
}

