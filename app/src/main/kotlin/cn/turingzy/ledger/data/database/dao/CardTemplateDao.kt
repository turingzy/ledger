package cn.turingzy.ledger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.turingzy.ledger.data.database.entity.CardTemplateEntity
import kotlinx.coroutines.flow.Flow

/**
 * 账户卡片样式模板数据访问对象 (DAO)。
 *
 * 管理银行卡、电子钱包等账户卡片的背景渐变、图标与主题样式预设。
 */
@Dao
interface CardTemplateDao {
    /**
     * 查询所有卡片模板记录（包含已删除），用于数据备份。
     */
    @Query("SELECT * FROM tb_card_template")
    suspend fun backupRows(): List<CardTemplateEntity>

    /**
     * 清空所有卡片模板记录（通常用于恢复备份前的数据清理）。
     */
    @Query("DELETE FROM tb_card_template")
    suspend fun clearRows()

    /**
     * 插入单个卡片模板，冲突时替换。
     *
     * @param template 卡片模板实体
     * @return 新模板的主键 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: CardTemplateEntity): Long

    /**
     * 批量插入卡片模板（预置数据导入时使用，忽略已存在的记录）。
     *
     * @param templates 模板实体列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTemplates(templates: List<CardTemplateEntity>)

    /**
     * 更新卡片模板信息。
     *
     * @param template 待更新的模板实体
     */
    @Update
    suspend fun updateTemplate(template: CardTemplateEntity)

    /**
     * 查询所有未被软删除的卡片模板列表流。
     *
     * @return 卡片模板列表流，按模板 ID 升序排列
     */
    @Query("SELECT * FROM tb_card_template WHERE is_deleted = 0 ORDER BY template_id ASC")
    fun getAllTemplates(): Flow<List<CardTemplateEntity>>

    /**
     * 观察指定 ID 的卡片模板。
     *
     * @param id 模板 ID
     * @return 卡片模板数据流，若已被软删除或不存在则为 null
     */
    @Query("SELECT * FROM tb_card_template WHERE template_id = :id AND is_deleted = 0")
    fun getTemplateById(id: Long): Flow<CardTemplateEntity?>

    /**
     * 一次性查询指定 ID 的未删除卡片模板。
     *
     * @param id 模板 ID
     * @return 卡片模板实体，不存在或已软删除则返回 null
     */
    @Query("SELECT * FROM tb_card_template WHERE template_id = :id AND is_deleted = 0")
    suspend fun getTemplateByIdOnce(id: Long): CardTemplateEntity?

    /**
     * 软删除指定卡片模板。
     *
     * @param id 模板 ID
     * @return 影响的行数
     */
    @Query("UPDATE tb_card_template SET is_deleted = 1 WHERE template_id = :id")
    suspend fun softDeleteTemplate(id: Long): Int
}

