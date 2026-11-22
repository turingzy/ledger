package cn.turingzy.ledger.data.database.converter

import androidx.room.TypeConverter
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.backup.BackupProviderType
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.transaction.TransactionType

/**
 * Room 数据库自定义类型转换器。
 *
 * 负责枚举类型与数据库中字符串形式之间的双向持久化转换。
 */
class RoomTypeConverters {

    /**
     * 将 [TransactionType] 枚举转换为字符串。
     */
    @TypeConverter
    fun transactionTypeToString(type: TransactionType?): String? = type?.name

    /**
     * 将字符串反序列化为 [TransactionType] 枚举。
     */
    @TypeConverter
    fun stringToTransactionType(value: String?): TransactionType? =
        value?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }

    /**
     * 将 [CategoryType] 枚举转换为字符串。
     */
    @TypeConverter
    fun categoryTypeToString(type: CategoryType?): String? = type?.name

    /**
     * 将字符串反序列化为 [CategoryType] 枚举。
     */
    @TypeConverter
    fun stringToCategoryType(value: String?): CategoryType? =
        value?.let { runCatching { CategoryType.valueOf(it) }.getOrNull() }

    /**
     * 将 [AccountType] 枚举转换为字符串。
     */
    @TypeConverter
    fun accountTypeToString(type: AccountType?): String? = type?.name

    /**
     * 将字符串反序列化为 [AccountType] 枚举。
     */
    @TypeConverter
    fun stringToAccountType(value: String?): AccountType? =
        value?.let { runCatching { AccountType.valueOf(it) }.getOrNull() }

    /**
     * 将 [BackupProviderType] 枚举转换为字符串。
     */
    @TypeConverter
    fun backupProviderTypeToString(type: BackupProviderType?): String? = type?.name

    /**
     * 将字符串反序列化为 [BackupProviderType] 枚举。
     */
    @TypeConverter
    fun stringToBackupProviderType(value: String?): BackupProviderType? =
        value?.let { runCatching { BackupProviderType.valueOf(it) }.getOrNull() }
}

