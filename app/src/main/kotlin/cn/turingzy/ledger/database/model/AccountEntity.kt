package cn.turingzy.ledger.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cn.turingzy.ledger.model.AccountType

@Entity(
    tableName = "tb_account",
    foreignKeys = [
        ForeignKey(
            entity = CardTemplateEntity::class,
            parentColumns = ["template_id"],
            childColumns = ["template_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("template_id"),
        Index(value = ["sort_order", "is_deleted"], name = "idx_account_sort"),
    ],
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "account_id")
    val accountId: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "type")
    val type: AccountType,
    @ColumnInfo(name = "initial_balance", defaultValue = "0")
    val initialBalance: Long = 0L,
    @ColumnInfo(name = "current_balance", defaultValue = "0")
    val currentBalance: Long = 0L,
    @ColumnInfo(name = "currency", defaultValue = "'CNY'")
    val currency: String = "CNY",
    @ColumnInfo(name = "template_id")
    val templateId: Long? = null,
    @ColumnInfo(name = "card_number")
    val cardNumber: String? = null,
    @ColumnInfo(name = "include_in_total", defaultValue = "1")
    val includeInTotal: Boolean = true,
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
)

