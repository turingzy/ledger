package cn.turingzy.ledger.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val ledgerDispatcher: LedgerDispatchers)

