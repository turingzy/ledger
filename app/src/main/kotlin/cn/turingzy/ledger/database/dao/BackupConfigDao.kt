package cn.turingzy.ledger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.turingzy.ledger.database.model.BackupConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: BackupConfigEntity): Long

    @Update
    suspend fun updateConfig(config: BackupConfigEntity)

    @Query("DELETE FROM tb_backup_config WHERE config_id = :id")
    suspend fun deleteConfig(id: Long): Int

    @Query("SELECT * FROM tb_backup_config ORDER BY config_id ASC")
    fun getAllConfigs(): Flow<List<BackupConfigEntity>>

    @Query("SELECT * FROM tb_backup_config WHERE config_id = :id")
    fun getConfigById(id: Long): Flow<BackupConfigEntity?>

    @Query("SELECT * FROM tb_backup_config WHERE config_id = :id")
    suspend fun getConfigByIdOnce(id: Long): BackupConfigEntity?

    @Query("SELECT * FROM tb_backup_config WHERE is_auto_backup = 1")
    fun getAutoBackupConfigs(): Flow<List<BackupConfigEntity>>

    @Query("UPDATE tb_backup_config SET last_backup_at = :timestamp WHERE config_id = :configId")
    suspend fun updateLastBackupTime(configId: Long, timestamp: Long): Int
}

