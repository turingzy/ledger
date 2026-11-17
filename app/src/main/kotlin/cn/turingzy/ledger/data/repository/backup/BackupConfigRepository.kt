package cn.turingzy.ledger.data.repository.backup

import cn.turingzy.ledger.model.backup.BackupConfig
import kotlinx.coroutines.flow.Flow

interface BackupConfigRepository {
    fun getConfigsStream(): Flow<List<BackupConfig>>
    fun getConfig(id: Long): Flow<BackupConfig?>
    fun getAutoBackupConfigsStream(): Flow<List<BackupConfig>>
    suspend fun createConfig(config: BackupConfig): Long
    suspend fun updateConfig(config: BackupConfig)
    suspend fun deleteConfig(id: Long)
    suspend fun updateLastBackupTime(configId: Long, timestamp: Long)
}

