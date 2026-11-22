package cn.turingzy.ledger.data.database.mapper

import cn.turingzy.ledger.data.database.entity.BackupConfigEntity
import cn.turingzy.ledger.model.backup.BackupConfig

/**
 * 将备份配置实体 [BackupConfigEntity] 转换为业务领域模型 [BackupConfig]。
 */
fun BackupConfigEntity.asExternalModel(): BackupConfig = BackupConfig(
    id = configId,
    name = name,
    providerType = providerType,
    endpointUrl = endpointUrl,
    accountIdentity = accountIdentity,
    encryptedSecret = encryptedSecret,
    remoteDirectory = remoteDirectory,
    isAutoBackup = isAutoBackup,
    lastBackupAt = lastBackupAt,
)

/**
 * 将业务领域模型 [BackupConfig] 转换为数据库实体 [BackupConfigEntity]。
 */
fun BackupConfig.asEntity(): BackupConfigEntity = BackupConfigEntity(
    configId = id,
    name = name,
    providerType = providerType,
    endpointUrl = endpointUrl,
    accountIdentity = accountIdentity,
    encryptedSecret = encryptedSecret,
    remoteDirectory = remoteDirectory,
    isAutoBackup = isAutoBackup,
    lastBackupAt = lastBackupAt,
)
