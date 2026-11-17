package cn.turingzy.ledger.data.database.converter

import androidx.room.TypeConverter
import cn.turingzy.ledger.model.account.AccountType
import cn.turingzy.ledger.model.backup.BackupProviderType
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.transaction.TransactionType

class RoomTypeConverters {

    @TypeConverter
    fun transactionTypeToString(type: TransactionType?): String? = type?.name

    @TypeConverter
    fun stringToTransactionType(value: String?): TransactionType? =
        value?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun categoryTypeToString(type: CategoryType?): String? = type?.name

    @TypeConverter
    fun stringToCategoryType(value: String?): CategoryType? =
        value?.let { runCatching { CategoryType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun accountTypeToString(type: AccountType?): String? = type?.name

    @TypeConverter
    fun stringToAccountType(value: String?): AccountType? =
        value?.let { runCatching { AccountType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun backupProviderTypeToString(type: BackupProviderType?): String? = type?.name

    @TypeConverter
    fun stringToBackupProviderType(value: String?): BackupProviderType? =
        value?.let { runCatching { BackupProviderType.valueOf(it) }.getOrNull() }
}

