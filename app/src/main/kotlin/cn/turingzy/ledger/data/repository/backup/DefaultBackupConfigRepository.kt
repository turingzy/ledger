package cn.turingzy.ledger.data.repository.backup

import cn.turingzy.ledger.data.database.dao.BackupConfigDao
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.data.database.mapper.asExternalModel
import cn.turingzy.ledger.model.backup.BackupConfig
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [BackupConfigRepository] 的默认实现。
 *
 * 通过 [BackupConfigDao] 在 IO 调度器上完成备份配置的本地数据存储与模型转换。
 *
 * @property backupConfigDao 备份配置数据访问对象
 * @property ioDispatcher 用于执行数据库操作的 IO 协程调度器
 */
class DefaultBackupConfigRepository @Inject constructor(
    private val backupConfigDao: BackupConfigDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : BackupConfigRepository {

    /**
     * 获取所有备份配置列表的流。
     */
    override fun getConfigsStream(): Flow<List<BackupConfig>> =
        backupConfigDao.getAllConfigs()
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    /**
     * 根据 ID 获取单个备份配置流。
     *
     * @param id 配置 ID
     */
    override fun getConfig(id: Long): Flow<BackupConfig?> =
        backupConfigDao.getConfigById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    /**
     * 获取启用了自动备份的配置列表流。
     */
    override fun getAutoBackupConfigsStream(): Flow<List<BackupConfig>> =
        backupConfigDao.getAutoBackupConfigs()
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    /**
     * 创建新的备份配置。
     *
     * @param config 待创建的备份配置
     * @return 新增配置的自增 ID
     */
    override suspend fun createConfig(config: BackupConfig): Long =
        withContext(ioDispatcher) {
            backupConfigDao.insertConfig(config.asEntity())
        }

    /**
     * 更新已有备份配置。
     *
     * @param config 待更新的备份配置
     */
    override suspend fun updateConfig(config: BackupConfig) {
        withContext(ioDispatcher) {
            backupConfigDao.updateConfig(config.asEntity())
        }
    }

    /**
     * 删除指定 ID 的备份配置。
     *
     * @param id 待删除的配置 ID
     */
    override suspend fun deleteConfig(id: Long) {
        withContext(ioDispatcher) {
            backupConfigDao.deleteConfig(id)
        }
    }

    /**
     * 更新备份配置的最近一次备份时间戳。
     *
     * @param configId 配置 ID
     * @param timestamp 备份时间（毫秒时间戳）
     */
    override suspend fun updateLastBackupTime(configId: Long, timestamp: Long) {
        withContext(ioDispatcher) {
            backupConfigDao.updateLastBackupTime(configId, timestamp)
        }
    }
}

