package cn.turingzy.ledger.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 账单导入解析规则数据库实体，对应表 `tb_parse_rule`。
 *
 * 存储 CSV/Excel 账单文件的列映射配置及默认绑定的账户与分类。
 *
 * @property ruleId 规则唯一标识，自增主键
 * @property name 规则显示名称（如“微信账单导出”、“支付宝账单导出”）
 * @property startRow 账单数据起始行号（从 1 开始，跳过表头）
 * @property mappingConfig 列映射规则的 JSON 序列化配置字符串
 * @property defaultAccountId 解析后默认分配的入账账户 ID
 * @property defaultCategoryId 解析后未命中规则时的默认兜底分类 ID
 * @property isSystem 是否为系统预置规则
 * @property isDeleted 软删除标记，true 表示已被软删除
 */
@Entity(
    tableName = "tb_parse_rule",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["account_id"],
            childColumns = ["default_account_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["category_id"],
            childColumns = ["default_category_id"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index("default_account_id"),
        Index("default_category_id"),
    ],
)
data class ParseRuleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "rule_id")
    val ruleId: Long = 0L,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "start_row", defaultValue = "1")
    val startRow: Int = 1,
    @ColumnInfo(name = "mapping_config")
    val mappingConfig: String,
    @ColumnInfo(name = "default_account_id")
    val defaultAccountId: Long? = null,
    @ColumnInfo(name = "default_category_id")
    val defaultCategoryId: Long? = null,
    @ColumnInfo(name = "is_system", defaultValue = "0")
    val isSystem: Boolean = false,
    @ColumnInfo(name = "is_deleted", defaultValue = "0")
    val isDeleted: Boolean = false,
)

