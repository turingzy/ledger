package cn.turingzy.ledger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.turingzy.ledger.database.model.ParseRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParseRuleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: ParseRuleEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRules(rules: List<ParseRuleEntity>)

    @Update
    suspend fun updateRule(rule: ParseRuleEntity)

    @Query("UPDATE tb_parse_rule SET is_deleted = 1 WHERE rule_id = :id")
    suspend fun softDeleteRule(id: Long): Int

    @Query("SELECT * FROM tb_parse_rule WHERE is_deleted = 0 ORDER BY rule_id ASC")
    fun getAllRules(): Flow<List<ParseRuleEntity>>

    @Query("SELECT * FROM tb_parse_rule WHERE rule_id = :id AND is_deleted = 0")
    fun getRuleById(id: Long): Flow<ParseRuleEntity?>

    @Query("SELECT * FROM tb_parse_rule WHERE rule_id = :id AND is_deleted = 0")
    suspend fun getRuleByIdOnce(id: Long): ParseRuleEntity?
}

