package cn.turingzy.ledger.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * 协程调度器依赖注入模块。
 *
 * 负责向依赖图提供带有限定符修饰的 [CoroutineDispatcher] 实例。
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    /**
     * 提供用于 IO 密集型操作（如数据库读写、文件解析及网络交互）的协程调度器。
     */
    @Provides
    @Dispatcher(LedgerDispatchers.IO)
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    /**
     * 提供用于 CPU 密集型任务（如数据计算、排序和统计）的标准协程调度器。
     */
    @Provides
    @Dispatcher(LedgerDispatchers.Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}

