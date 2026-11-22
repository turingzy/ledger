package cn.turingzy.ledger.di

import javax.inject.Qualifier

/**
 * 协程调度器限定符注解。
 *
 * 用于在依赖注入中区分不同用途的 [kotlinx.coroutines.CoroutineDispatcher]，
 * 如 [LedgerDispatchers.IO] 或 [LedgerDispatchers.DEFAULT]。
 *
 * @property ledgerDispatcher 指定的调度器类型。
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val ledgerDispatcher: LedgerDispatchers)

