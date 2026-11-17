package cn.turingzy.ledger.data.database.mapper

import cn.turingzy.ledger.data.database.entity.BackupConfigEntity
import cn.turingzy.ledger.model.backup.BackupConfig

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
