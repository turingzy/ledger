package cn.turingzy.ledger.model

import kotlinx.serialization.Serializable

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

