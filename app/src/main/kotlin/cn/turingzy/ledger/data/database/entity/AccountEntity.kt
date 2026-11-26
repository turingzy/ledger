package cn.turingzy.ledger.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import cn.turingzy.ledger.model.account.AccountType

/**
 * 账户数据库实体，对应表 `tb_account`。
 *
 * 存储资金账户的基本信息、币种、余额及基准货币估值缓存。
 *
 * @property accountId 账户唯一标识，自增主键
 * @property name 账户名称（如“招商银行卡”、“微信零钱”）
 * @property type 账户类型（如银行卡、电子钱包、信用账户等）
 * @property initialBalance 初始余额（分）
 * @property currentBalance 当前余额（分）
 * @property currency 账户货币代码（ISO 4217，如 CNY、USD）
 * @property templateId 关联的卡片样式模板 ID
 * @property cardNumber 卡号后四位或自定义标识
 * @property includeInTotal 是否计入净资产统计
 * @property sortOrder 排序权重（保留用于数据库兼容性，当前列表按 account_id 排序）
 * @property isDeleted 软删除标记，true 表示已被软删除
 * @property baseBalance 折算为基准货币的估值余额缓存（分）
 * @property baseCurrency 估值使用的基准货币代码
 * @property baseRate 估值计算时使用的汇率字符串
 * @property baseRateDate 估值汇率的生效日期（格式：YYYY-MM-DD）
 */
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
    // Retained for database compatibility; account display order uses account_id.
    @ColumnInfo(name = "sort_order", defaultValue = "0")
    val sortOrder: Int = 0,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
    @ColumnInfo(name = "base_balance")
    val baseBalance: Long? = null,
    @ColumnInfo(name = "base_currency")
    val baseCurrency: String? = null,
    @ColumnInfo(name = "base_rate")
    val baseRate: String? = null,
    @ColumnInfo(name = "base_rate_date")
    val baseRateDate: String? = null,
    @ColumnInfo(name = "encrypted_card_details")
    val encryptedCardDetails: String? = null,
)

