package cn.turingzy.ledger.di

/**
 * 协程调度器类型枚举。
 *
 * 配合 [Dispatcher] 限定符使用，用于指定注入哪类协程调度器。
 */
enum class LedgerDispatchers {
    /** 适用于 CPU 密集型计算任务的调度器。 */
    Default,

    /** 适用于磁盘读写、数据库事务与网络请求等 IO 密集型操作的调度器。 */
    IO,
}

