package cn.turingzy.ledger.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cn.turingzy.ledger.model.TransactionType

@Entity(
    tableName = "tb_transaction",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["account_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["to_account_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(
            value = ["is_deleted", "transaction_time"],
            orders = [Index.Order.ASC, Index.Order.DESC],
            name = "idx_transaction_time_deleted",
        ),
        Index(
            value = ["account_id", "is_deleted"],
            name = "idx_transaction_account",
        ),
        Index(
            value = ["to_account_id", "is_deleted"],
            name = "idx_transaction_to_account",
        ),
        Index(
            value = ["category_id", "is_deleted", "transaction_time"],
            name = "idx_transaction_category_time",
        ),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "transaction_id")
    val transactionId: Long = 0L,
    @ColumnInfo(name = "type")
    val type: TransactionType,
    @ColumnInfo(name = "amount")
    val amount: Long,
    @ColumnInfo(name = "account_id")
    val accountId: Long,
    @ColumnInfo(name = "to_account_id")
    val toAccountId: Long? = null,
    @ColumnInfo(name = "category_id")
    val categoryId: Long,
    @ColumnInfo(name = "transaction_time")
    val transactionTime: Long,
    @ColumnInfo(name = "note")
    val note: String? = null,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
)

