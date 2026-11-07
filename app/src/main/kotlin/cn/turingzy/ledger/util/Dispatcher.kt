package cn.turingzy.ledger.util

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val ledgerDispatcher: LedgerDispatchers)

