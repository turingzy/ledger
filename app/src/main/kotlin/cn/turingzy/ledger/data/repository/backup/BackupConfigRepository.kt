package cn.turingzy.ledger.data.repository.backup

import cn.turingzy.ledger.model.backup.BackupConfig
import kotlinx.coroutines.flow.Flow

/**
 * 备份配置数据仓库接口。
 *
 * 提供自动与手动备份目标配置的增删改查及备份状态更新能力。
 */
interface BackupConfigRepository {
    /**
     * 获取所有备份配置列表的响应式流。
     */
    fun getConfigsStream(): Flow<List<BackupConfig>>

    /**
     * 根据配置 ID 获取单个备份配置流。
     *
     * @param id 配置 ID
     * @return 匹配的备份配置流；若未找到则发射 null
     */
    fun getConfig(id: Long): Flow<BackupConfig?>

    /**
     * 获取所有启用了自动备份的配置列表流。
     */
    fun getAutoBackupConfigsStream(): Flow<List<BackupConfig>>

    /**
     * 创建新的备份配置。
     *
     * @param config 待创建的备份配置
     * @return 新增配置的自增 ID
     */
    suspend fun createConfig(config: BackupConfig): Long

    /**
     * 更新已有备份配置。
     *
     * @param config 待更新的备份配置
     */
    suspend fun updateConfig(config: BackupConfig)

    /**
     * 删除指定 ID 的备份配置。
     *
     * @param id 待删除的配置 ID
     */
    suspend fun deleteConfig(id: Long)

    /**
     * 更新指定备份配置的最近一次备份时间戳。
     *
     * @param configId 配置 ID
     * @param timestamp 最近备份时间（毫秒时间戳）
     */
    suspend fun updateLastBackupTime(configId: Long, timestamp: Long)
}

