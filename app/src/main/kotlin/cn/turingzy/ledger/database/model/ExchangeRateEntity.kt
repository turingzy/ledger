package cn.turingzy.ledger.database.model

import androidx.room.Entity

@Entity(tableName = "tb_exchange_rate", primaryKeys = ["date", "currency"])
data class ExchangeRateEntity(val date: String, val currency: String, val perUsd: String, val manual: Boolean = false)
