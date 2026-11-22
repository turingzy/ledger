package cn.turingzy.ledger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import cn.turingzy.ledger.data.database.entity.IconEntity
import kotlinx.coroutines.flow.Flow

/**
 * 系统图标数据访问对象 (DAO)。
 *
 * 管理分类和账户等实体引用的矢量图标或图片资源名称。
 */
@Dao
interface IconDao {
    /**
     * 查询所有图标记录（包含已删除），用于数据备份。
     */
    @Query("SELECT * FROM tb_icon")
    suspend fun backupRows(): List<IconEntity>

    /**
     * 清空所有图标记录（通常用于恢复备份前的数据清理）。
     */
    @Query("DELETE FROM tb_icon")
    suspend fun clearRows()

    /**
     * 插入单个图标，主键冲突时替换。
     *
     * @param icon 图标实体
     * @return 新图标的主键 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIcon(icon: IconEntity): Long

    /**
     * 批量插入图标（初始化种子数据，冲突时忽略）。
     *
     * @param icons 图标实体列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIcons(icons: List<IconEntity>)

    /**
     * 更新图标信息。
     *
     * @param icon 待更新的图标实体
     */
    @Update
    suspend fun updateIcon(icon: IconEntity)

    /**
     * 获取所有未删除图标的列表流。
     *
     * 按排序权重 [IconEntity.sortOrder] 升序及 ID 升序排列。
     */
    @Query("SELECT * FROM tb_icon WHERE is_deleted = 0 ORDER BY sort_order ASC, icon_id ASC")
    fun getAllIcons(): Flow<List<IconEntity>>

    /**
     * 观察指定 ID 的未删除图标。
     *
     * @param id 图标 ID
     * @return 图标实体流，若不存在或已删除则为 null
     */
    @Query("SELECT * FROM tb_icon WHERE icon_id = :id AND is_deleted = 0")
    fun getIconById(id: Long): Flow<IconEntity?>

    /**
     * 一次性查询未删除的指定图标实体。
     *
     * @param id 图标 ID
     * @return 图标实体，不存在或已删除则返回 null
     */
    @Query("SELECT * FROM tb_icon WHERE icon_id = :id AND is_deleted = 0")
    suspend fun getIconByIdOnce(id: Long): IconEntity?

    /**
     * 软删除指定图标。
     *
     * @param id 图标 ID
     * @return 影响的行数
     */
    @Query("UPDATE tb_icon SET is_deleted = 1 WHERE icon_id = :id")
    suspend fun softDeleteIcon(id: Long): Int
}

