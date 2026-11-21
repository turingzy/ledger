package cn.turingzy.ledger.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "tb_exchange_rate", primaryKeys = ["date", "currency"])
data class ExchangeRateEntity(
    val date: String,
    val currency: String,
    val perUsd: String,
    val manual: Boolean = false,
    @ColumnInfo(name = "updated_at_millis") val updatedAtMillis: Long? = null,
)
