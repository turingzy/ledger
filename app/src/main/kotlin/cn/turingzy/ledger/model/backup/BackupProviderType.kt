package cn.turingzy.ledger.model.backup

import kotlinx.serialization.Serializable

/**
 * 备份服务提供商类型枚举。
 *
 * 定义支持的远端备份协议及云存储服务。
 */
@Serializable
enum class BackupProviderType {
    /**
     * WebDAV 协议网盘（如坚果云、Nextcloud 等）。
     */
    WEBDAV,

    /**
     * 阿里云对象存储（Aliyun OSS）。
     */
    ALIYUN_OSS,

    /**
     * AWS S3 或兼容 S3 协议的对象存储（如 Cloudflare R2、MinIO 等）。
     */
    S3,

    /**
     * 自定义 REST 接口备份服务。
     */
    CUSTOM_REST,
}

