package cn.turingzy.ledger.model.backup

import kotlinx.serialization.Serializable

/**
 * 云端备份配置数据模型。
 *
 * 记录用户配置的远端备份服务连接参数、认证信息与备份状态。
 *
 * @property id 备份配置唯一标识，自增主键
 * @property name 配置名称（如“我的 WebDAV 备份”）
 * @property providerType 备份服务提供商类型，详见 [BackupProviderType]
 * @property endpointUrl 服务端点地址（如 WebDAV 根 URL 或 OSS Endpoint）
 * @property accountIdentity 账户标识（如用户名或 AccessKey ID），可为空
 * @property encryptedSecret 加密存储的凭据密钥（如密码或 SecretAccessKey）
 * @property remoteDirectory 远端存储目录路径（默认为 "/"）
 * @property isAutoBackup 是否启用自动备份
 * @property lastBackupAt 上次成功备份的时间戳（毫秒），未备份过则为 null
 */
@Serializable
data class BackupConfig(
    val id: Long = 0L,
    val name: String,
    val providerType: BackupProviderType,
    val endpointUrl: String,
    val accountIdentity: String? = null,
    val encryptedSecret: String,
    val remoteDirectory: String = "/",
    val isAutoBackup: Boolean = false,
    val lastBackupAt: Long? = null,
)

