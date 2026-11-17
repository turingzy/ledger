package cn.turingzy.ledger.model.backup

import kotlinx.serialization.Serializable

@Serializable
enum class BackupProviderType {
    WEBDAV,
    ALIYUN_OSS,
    S3,
    CUSTOM_REST,
}

