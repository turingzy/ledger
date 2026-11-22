package cn.turingzy.ledger.data.database.dao

import androidx.room.*
import cn.turingzy.ledger.data.database.entity.ExchangeRateEntity

/**
 * 历史汇率数据访问对象 (DAO)。
 *
 * 管理从网络同步或缓存的货币兑换汇率数据表。
 */
@Dao
interface ExchangeRateDao {
    /**
     * 查询所有汇率记录，按生效日期升序排列。
     */
    @Query("SELECT * FROM tb_exchange_rate ORDER BY date")
    suspend fun rows(): List<ExchangeRateEntity>

    /**
     * 清空汇率表中的所有记录。
     */
    @Query("DELETE FROM tb_exchange_rate")
    suspend fun clear()

    /**
     * 批量插入或替换汇率记录。
     *
     * @param rows 汇率实体列表
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rows: List<ExchangeRateEntity>)
}
