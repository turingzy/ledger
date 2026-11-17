package cn.turingzy.ledger.di

import cn.turingzy.ledger.data.repository.account.AccountRepository
import cn.turingzy.ledger.data.repository.backup.BackupConfigRepository
import cn.turingzy.ledger.data.repository.account.CardTemplateRepository
import cn.turingzy.ledger.data.repository.category.CategoryRepository
import cn.turingzy.ledger.data.repository.account.DefaultAccountRepository
import cn.turingzy.ledger.data.repository.backup.DefaultBackupConfigRepository
import cn.turingzy.ledger.data.repository.account.DefaultCardTemplateRepository
import cn.turingzy.ledger.data.repository.category.DefaultCategoryRepository
import cn.turingzy.ledger.data.repository.category.DefaultIconRepository
import cn.turingzy.ledger.data.repository.settings.DefaultParseRuleRepository
import cn.turingzy.ledger.data.repository.transaction.DefaultTransactionRepository
import cn.turingzy.ledger.data.repository.category.IconRepository
import cn.turingzy.ledger.data.repository.settings.ParseRuleRepository
import cn.turingzy.ledger.data.repository.transaction.TransactionRepository
import cn.turingzy.ledger.data.repository.settings.DefaultLedgerCardAppearanceRepository
import cn.turingzy.ledger.data.repository.settings.LedgerCardAppearanceRepository
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
    fun bindsExchangeRateSource(impl: cn.turingzy.ledger.data.repository.currency.FrankfurterExchangeRateSource): cn.turingzy.ledger.data.repository.currency.ExchangeRateSource

    @Binds
    @Singleton
    fun bindsAppUpdateRepository(
        impl: cn.turingzy.ledger.data.repository.settings.DefaultAppUpdateRepository,
    ): cn.turingzy.ledger.data.repository.settings.AppUpdateRepository

    @Binds
    @Singleton
    fun bindsSettingsDataRepository(
        impl: cn.turingzy.ledger.data.repository.settings.DefaultSettingsDataRepository,
    ): cn.turingzy.ledger.data.repository.settings.SettingsDataRepository


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

