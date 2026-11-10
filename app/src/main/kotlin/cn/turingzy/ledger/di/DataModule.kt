package cn.turingzy.ledger.di

import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.data.repository.BackupConfigRepository
import cn.turingzy.ledger.data.repository.CardTemplateRepository
import cn.turingzy.ledger.data.repository.CategoryRepository
import cn.turingzy.ledger.data.repository.DefaultAccountRepository
import cn.turingzy.ledger.data.repository.DefaultBackupConfigRepository
import cn.turingzy.ledger.data.repository.DefaultCardTemplateRepository
import cn.turingzy.ledger.data.repository.DefaultCategoryRepository
import cn.turingzy.ledger.data.repository.DefaultIconRepository
import cn.turingzy.ledger.data.repository.DefaultParseRuleRepository
import cn.turingzy.ledger.data.repository.DefaultTransactionRepository
import cn.turingzy.ledger.data.repository.IconRepository
import cn.turingzy.ledger.data.repository.ParseRuleRepository
import cn.turingzy.ledger.data.repository.TransactionRepository
import cn.turingzy.ledger.data.repository.DefaultLedgerCardAppearanceRepository
import cn.turingzy.ledger.data.repository.LedgerCardAppearanceRepository
import cn.turingzy.ledger.data.repository.AnalyticsCardSettingsRepository
import cn.turingzy.ledger.data.repository.DefaultAnalyticsCardSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Binds
    @Singleton
    fun bindsAnalyticsCardSettingsRepository(
        impl: DefaultAnalyticsCardSettingsRepository,
    ): AnalyticsCardSettingsRepository


    @Binds
    @Singleton
    fun bindsLedgerCardAppearanceRepository(
        impl: DefaultLedgerCardAppearanceRepository,
    ): LedgerCardAppearanceRepository

    @Binds
    @Singleton
    fun bindsTransactionRepository(
        impl: DefaultTransactionRepository,
    ): TransactionRepository

    @Binds
    @Singleton
    fun bindsAccountRepository(
        impl: DefaultAccountRepository,
    ): AccountRepository

    @Binds
    @Singleton
    fun bindsCategoryRepository(
        impl: DefaultCategoryRepository,
    ): CategoryRepository

    @Binds
    @Singleton
    fun bindsIconRepository(
        impl: DefaultIconRepository,
    ): IconRepository

    @Binds
    @Singleton
    fun bindsCardTemplateRepository(
        impl: DefaultCardTemplateRepository,
    ): CardTemplateRepository

    @Binds
    @Singleton
    fun bindsParseRuleRepository(
        impl: DefaultParseRuleRepository,
    ): ParseRuleRepository

    @Binds
    @Singleton
    fun bindsBackupConfigRepository(
        impl: DefaultBackupConfigRepository,
    ): BackupConfigRepository
}

