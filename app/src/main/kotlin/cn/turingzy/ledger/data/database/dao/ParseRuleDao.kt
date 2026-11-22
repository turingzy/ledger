package cn.turingzy.ledger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.turingzy.ledger.data.database.entity.ParseRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * 账单短信/通知解析规则数据访问对象 (DAO)。
 *
 * 维护通过正则表达式自动匹配并提取交易信息的规则数据。
 */
@Dao
interface ParseRuleDao {
    /**
     * 查询所有解析规则记录（包含已删除），用于数据备份。
     */
    @Query("SELECT * FROM tb_parse_rule")
    suspend fun backupRows(): List<ParseRuleEntity>

    /**
     * 清空所有解析规则记录（通常用于恢复备份前的数据清理）。
     */
    @Query("DELETE FROM tb_parse_rule")
    suspend fun clearRows()

    /**
     * 插入单个解析规则，主键冲突时替换。
     *
     * @param rule 解析规则实体
     * @return 新规则的主键 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ParseRuleEntity): Long

    /**
     * 批量插入解析规则（冲突时忽略）。
     *
     * @param rules 解析规则实体列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRules(rules: List<ParseRuleEntity>)

    /**
     * 更新解析规则。
     *
     * @param rule 待更新的解析规则实体
     */
    @Update
    suspend fun updateRule(rule: ParseRuleEntity)

    /**
     * 软删除指定解析规则。
     *
     * @param id 规则 ID
     * @return 影响的行数
     */
    @Query("UPDATE tb_parse_rule SET is_deleted = 1 WHERE rule_id = :id")
    suspend fun softDeleteRule(id: Long): Int

    /**
     * 查询所有未删除的解析规则列表流。
     *
     * @return 解析规则列表流，按规则 ID 升序排列
     */
    @Query("SELECT * FROM tb_parse_rule WHERE is_deleted = 0 ORDER BY rule_id ASC")
    fun getAllRules(): Flow<List<ParseRuleEntity>>

    /**
     * 观察指定 ID 的未删除解析规则。
     *
     * @param id 规则 ID
     * @return 解析规则流，不存在或已软删除则为 null
     */
    @Query("SELECT * FROM tb_parse_rule WHERE rule_id = :id AND is_deleted = 0")
    fun getRuleById(id: Long): Flow<ParseRuleEntity?>

    /**
     * 一次性查询未删除的指定解析规则。
     *
     * @param id 规则 ID
     * @return 解析规则实体，不存在或已软删除则返回 null
     */
    @Query("SELECT * FROM tb_parse_rule WHERE rule_id = :id AND is_deleted = 0")
    suspend fun getRuleByIdOnce(id: Long): ParseRuleEntity?
}

