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

/**
 * 数据层依赖注入模块。
 *
 * 负责将数据仓储（Repository）和数据源接口与对应的默认实现进行单例绑定。
 */
@Module
@InstallIn(SingletonComponent::class)
interface DataModule {
    /**
     * 绑定汇率数据源实现。
     */
    @Binds
    @Singleton
    fun bindsExchangeRateSource(impl: cn.turingzy.ledger.data.repository.currency.FrankfurterExchangeRateSource): cn.turingzy.ledger.data.repository.currency.ExchangeRateSource

    /**
     * 绑定应用版本检查与更新仓储实现。
     */
    @Binds
    @Singleton
    fun bindsAppUpdateRepository(
        impl: cn.turingzy.ledger.data.repository.settings.DefaultAppUpdateRepository,
    ): cn.turingzy.ledger.data.repository.settings.AppUpdateRepository

    /**
     * 绑定应用通用设置与偏好数据仓储实现。
     */
    @Binds
    @Singleton
    fun bindsSettingsDataRepository(
        impl: cn.turingzy.ledger.data.repository.settings.DefaultSettingsDataRepository,
    ): cn.turingzy.ledger.data.repository.settings.SettingsDataRepository

    /**
     * 绑定账本卡片外观配置仓储实现。
     */
    @Binds
    @Singleton
    fun bindsLedgerCardAppearanceRepository(
        impl: DefaultLedgerCardAppearanceRepository,
    ): LedgerCardAppearanceRepository

    /**
     * 绑定账单交易记录仓储实现。
     */
    @Binds
    @Singleton
    fun bindsTransactionRepository(
        impl: DefaultTransactionRepository,
    ): TransactionRepository

    /**
     * 绑定资金账户仓储实现。
     */
    @Binds
    @Singleton
    fun bindsAccountRepository(
        impl: DefaultAccountRepository,
    ): AccountRepository

    /**
     * 绑定收支分类仓储实现。
     */
    @Binds
    @Singleton
    fun bindsCategoryRepository(
        impl: DefaultCategoryRepository,
    ): CategoryRepository

    /**
     * 绑定分类图标仓储实现。
     */
    @Binds
    @Singleton
    fun bindsIconRepository(
        impl: DefaultIconRepository,
    ): IconRepository

    /**
     * 绑定账户卡面背景模板仓储实现。
     */
    @Binds
    @Singleton
    fun bindsCardTemplateRepository(
        impl: DefaultCardTemplateRepository,
    ): CardTemplateRepository

    /**
     * 绑定账单文件解析规则仓储实现。
     */
    @Binds
    @Singleton
    fun bindsParseRuleRepository(
        impl: DefaultParseRuleRepository,
    ): ParseRuleRepository

    /**
     * 绑定数据备份与恢复配置仓储实现。
     */
    @Binds
    @Singleton
    fun bindsBackupConfigRepository(
        impl: DefaultBackupConfigRepository,
    ): BackupConfigRepository
}

