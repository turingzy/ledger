package cn.turingzy.ledger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.turingzy.ledger.data.database.entity.BackupConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * 备份配置数据访问对象 (DAO)。
 *
 * 管理 WebDAV 等外部存储的自动/手动备份策略配置记录。
 */
@Dao
interface BackupConfigDao {

    /**
     * 插入或替换备份配置。
     *
     * @param config 备份配置实体
     * @return 新配置的主键 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: BackupConfigEntity): Long

    /**
     * 更新现有备份配置。
     *
     * @param config 待更新的备份配置实体
     */
    @Update
    suspend fun updateConfig(config: BackupConfigEntity)

    /**
     * 根据主键 ID 删除备份配置。
     *
     * @param id 配置 ID
     * @return 影响的行数
     */
    @Query("DELETE FROM tb_backup_config WHERE config_id = :id")
    suspend fun deleteConfig(id: Long): Int

    /**
     * 查询所有备份配置，按配置 ID 升序排列。
     *
     * @return 备份配置列表的数据流
     */
    @Query("SELECT * FROM tb_backup_config ORDER BY config_id ASC")
    fun getAllConfigs(): Flow<List<BackupConfigEntity>>

    /**
     * 观察指定 ID 的备份配置变更。
     *
     * @param id 配置 ID
     * @return 备份配置的数据流
     */
    @Query("SELECT * FROM tb_backup_config WHERE config_id = :id")
    fun getConfigById(id: Long): Flow<BackupConfigEntity?>

    /**
     * 一次性查询指定 ID 的备份配置。
     *
     * @param id 配置 ID
     * @return 备份配置实体，不存在则返回 null
     */
    @Query("SELECT * FROM tb_backup_config WHERE config_id = :id")
    suspend fun getConfigByIdOnce(id: Long): BackupConfigEntity?

    /**
     * 查询所有启用了自动备份的配置列表。
     *
     * @return 开启自动备份的配置列表数据流
     */
    @Query("SELECT * FROM tb_backup_config WHERE is_auto_backup = 1")
    fun getAutoBackupConfigs(): Flow<List<BackupConfigEntity>>

    /**
     * 更新指定配置的最后一次成功备份时间戳。
     *
     * @param configId 配置 ID
     * @param timestamp 备份时间戳（毫秒）
     * @return 影响的行数
     */
    @Query("UPDATE tb_backup_config SET last_backup_at = :timestamp WHERE config_id = :configId")
    suspend fun updateLastBackupTime(configId: Long, timestamp: Long): Int
}

