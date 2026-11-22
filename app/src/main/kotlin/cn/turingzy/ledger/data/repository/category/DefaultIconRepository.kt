package cn.turingzy.ledger.data.repository.category

import cn.turingzy.ledger.data.database.dao.IconDao
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.data.database.mapper.asExternalModel
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [IconRepository] 的默认实现。
 *
 * 通过 [IconDao] 在 IO 调度器上实现图标的持久化与模型转换。
 *
 * @property iconDao 图标数据访问对象
 * @property ioDispatcher 用于执行数据库操作的 IO 协程调度器
 */
class DefaultIconRepository @Inject constructor(
    private val iconDao: IconDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : IconRepository {

    /**
     * 获取所有可用图标列表的流。
     */
    override fun getIconsStream(): Flow<List<Icon>> =
        iconDao.getAllIcons()
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    /**
     * 根据 ID 获取单个图标流。
     *
     * @param id 图标 ID
     */
    override fun getIcon(id: Long): Flow<Icon?> =
        iconDao.getIconById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    /**
     * 创建新图标。
     *
     * @param icon 待创建的图标
     * @return 新增图标自增 ID
     */
    override suspend fun createIcon(icon: Icon): Long =
        withContext(ioDispatcher) {
            iconDao.insertIcon(icon.asEntity())
        }

    /**
     * 软删除指定 ID 的图标。
     *
     * @param id 待删除的图标 ID
     */
    override suspend fun deleteIcon(id: Long) {
        withContext(ioDispatcher) {
            iconDao.softDeleteIcon(id)
        }
    }
}

