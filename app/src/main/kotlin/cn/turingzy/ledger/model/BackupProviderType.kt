package cn.turingzy.ledger.model

import kotlinx.serialization.Serializable

@Serializable
enum class BackupProviderType {
    WEBDAV,
    ALIYUN_OSS,
    S3,
    CUSTOM_REST,
}

