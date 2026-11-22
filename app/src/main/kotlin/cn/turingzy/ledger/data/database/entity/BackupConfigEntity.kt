package cn.turingzy.ledger.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.turingzy.ledger.model.backup.BackupProviderType

/**
 * 备份配置数据库实体，对应表 `tb_backup_config`。
 *
 * 存储远端云存储（如 WebDAV）的连接端点、认证凭证与自动同步设置。
 *
 * @property configId 配置唯一标识，自增主键
 * @property name 配置显示名称（如“坚果云 WebDAV”）
 * @property providerType 备份服务商类型 [BackupProviderType]
 * @property endpointUrl 远端服务器接入地址 URL
 * @property accountIdentity 认证账号或用户名
 * @property encryptedSecret 加密存储的应用密码或访问令牌
 * @property remoteDirectory 远端存储的目标根目录路径
 * @property isAutoBackup 是否启用发生变动时的自动备份
 * @property lastBackupAt 最近一次成功备份的时间戳（毫秒）
 */
@Entity(tableName = "tb_backup_config")
data class BackupConfigEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "config_id")
    val configId: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "provider_type")
    val providerType: BackupProviderType,
    @ColumnInfo(name = "endpoint_url")
    val endpointUrl: String,
    @ColumnInfo(name = "account_identity")
    val accountIdentity: String? = null,
    @ColumnInfo(name = "encrypted_secret")
    val encryptedSecret: String,
    @ColumnInfo(name = "remote_directory", defaultValue = "'/'")
    val remoteDirectory: String = "/",
    @ColumnInfo(name = "is_auto_backup", defaultValue = "0")
    val isAutoBackup: Boolean = false,
    @ColumnInfo(name = "last_backup_at")
    val lastBackupAt: Long? = null,
)

