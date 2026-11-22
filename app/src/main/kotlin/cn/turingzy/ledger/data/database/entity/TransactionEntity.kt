package cn.turingzy.ledger.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cn.turingzy.ledger.model.transaction.TransactionType

/**
 * 交易记录数据库实体，对应表 `tb_transaction`。
 *
 * 存储每笔支出、收入或转账的原始金额、发生账户、分类、时间戳以及汇率估值和快照信息。
 *
 * @property transactionId 交易唯一标识，自增主键
 * @property type 交易类型（支出、收入、转账）[TransactionType]
 * @property amount 交易原始金额（分，以源账户货币计）
 * @property accountId 源账户 ID
 * @property toAccountId 转账目标账户 ID，非转账交易时为 null
 * @property categoryId 交易分类 ID
 * @property transactionTime 交易发生时间戳（毫秒）
 * @property note 交易备注或商家描述信息
 * @property isDeleted 软删除标记，true 表示已被软删除
 * @property importKey 导入去重键，用于账单文件重复导入校验
 * @property baseAmount 折算为系统基准货币的估值金额缓存（分）
 * @property baseCurrency 估值时使用的基准货币代码
 * @property baseRate 源货币对基准货币的估值汇率
 * @property baseRateDate 估值汇率生效日期（YYYY-MM-DD）
 * @property toAmount 跨币种转账时目标账户实际入账金额（分）
 * @property transferRate 跨币种转账时的折算汇率
 * @property currencySnapshots 交易发生时的货币信息与汇率快照 JSON 字符串
 */
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
    @ColumnInfo(name = "import_key")
    val importKey: String? = null,
    @ColumnInfo(name = "base_amount")
    val baseAmount: Long? = null,
    @ColumnInfo(name = "base_currency")
    val baseCurrency: String? = null,
    @ColumnInfo(name = "base_rate")
    val baseRate: String? = null,
    @ColumnInfo(name = "base_rate_date")
    val baseRateDate: String? = null,
    @ColumnInfo(name = "to_amount")
    val toAmount: Long? = null,
    @ColumnInfo(name = "transfer_rate")
    val transferRate: String? = null,
    @ColumnInfo(name = "currency_snapshots", defaultValue = "'[]'")
    val currencySnapshots: String = "[]",
)

