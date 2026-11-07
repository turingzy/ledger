package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.database.dao.BackupConfigDao
import cn.turingzy.ledger.database.mapper.asEntity
import cn.turingzy.ledger.database.mapper.asExternalModel
import cn.turingzy.ledger.model.BackupConfig
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultBackupConfigRepository @Inject constructor(
    private val backupConfigDao: BackupConfigDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : BackupConfigRepository {

    override fun getConfigsStream(): Flow<List<BackupConfig>> =
        backupConfigDao.getAllConfigs()
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override fun getConfig(id: Long): Flow<BackupConfig?> =
        backupConfigDao.getConfigById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    override fun getAutoBackupConfigsStream(): Flow<List<BackupConfig>> =
        backupConfigDao.getAutoBackupConfigs()
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override suspend fun createConfig(config: BackupConfig): Long =
        withContext(ioDispatcher) {
            backupConfigDao.insertConfig(config.asEntity())
        }

    override suspend fun updateConfig(config: BackupConfig) {
        withContext(ioDispatcher) {
            backupConfigDao.updateConfig(config.asEntity())
        }
    }

    override suspend fun deleteConfig(id: Long) {
        withContext(ioDispatcher) {
            backupConfigDao.deleteConfig(id)
        }
    }

    override suspend fun updateLastBackupTime(configId: Long, timestamp: Long) {
        withContext(ioDispatcher) {
            backupConfigDao.updateLastBackupTime(configId, timestamp)
        }
    }
}

