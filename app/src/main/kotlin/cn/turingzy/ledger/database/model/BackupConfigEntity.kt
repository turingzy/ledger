package cn.turingzy.ledger.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import cn.turingzy.ledger.model.BackupProviderType

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

