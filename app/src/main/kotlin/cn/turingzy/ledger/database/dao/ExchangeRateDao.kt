package cn.turingzy.ledger.database.dao

import androidx.room.*
import cn.turingzy.ledger.database.model.ExchangeRateEntity

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM tb_exchange_rate ORDER BY date")
    suspend fun rows(): List<ExchangeRateEntity>
    @Query("DELETE FROM tb_exchange_rate")
    suspend fun clear()
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rows: List<ExchangeRateEntity>)
}
