package cn.turingzy.ledger.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * 历史汇率数据库实体，对应表 `tb_exchange_rate`。
 *
 * 复合主键为 `[date, currency]`，记录某一天特定货币相对于 USD 的兑换基准汇率。
 *
 * @property date 汇率生效日期（格式：YYYY-MM-DD）
 * @property currency 货币代码（ISO 4217，如 CNY、JPY）
 * @property perUsd 1 美元可兑换的目标货币数值字符串
 * @property manual 是否为用户手动录入的自定义汇率
 * @property updatedAtMillis 汇率记录的最后更新时间戳（毫秒）
 */
@Entity(tableName = "tb_exchange_rate", primaryKeys = ["date", "currency"])
data class ExchangeRateEntity(
    val date: String,
    val currency: String,
    val perUsd: String,
    val manual: Boolean = false,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long? = null,
)
