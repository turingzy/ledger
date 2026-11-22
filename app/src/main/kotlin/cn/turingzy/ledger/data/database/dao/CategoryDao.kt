package cn.turingzy.ledger.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cn.turingzy.ledger.data.database.entity.CategoryEntity
import cn.turingzy.ledger.data.database.entity.PopulatedCategory
import cn.turingzy.ledger.model.category.CategoryType
import kotlinx.coroutines.flow.Flow

/**
 * 交易分类数据访问对象 (DAO)。
 *
 * 提供支出、收入等分类实体的增删改查及带图标关联的聚合查询功能。
 */
@Dao
interface CategoryDao {
    /**
     * 查询所有分类记录（包含已删除），用于数据备份。
     */
    @Query("SELECT * FROM tb_category")
    suspend fun backupRows(): List<CategoryEntity>

    /**
     * 清空所有分类记录（通常用于恢复备份前的数据清理）。
     */
    @Query("DELETE FROM tb_category")
    suspend fun clearRows()

    /**
     * 插入单个分类，若主键冲突则替换。
     *
     * @param category 分类实体
     * @return 新分类的主键 ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    /**
     * 批量插入分类（用于初始化种子数据，冲突时忽略）。
     *
     * @param categories 分类实体列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    /**
     * 更新分类实体。
     *
     * @param category 待更新的分类实体
     */
    @Update
    suspend fun updateCategory(category: CategoryEntity)

    /**
     * 软删除指定分类。
     *
     * @param id 分类 ID
     * @return 影响的行数
     */
    @Query("UPDATE tb_category SET is_deleted = 1 WHERE category_id = :id")
    suspend fun softDeleteCategory(id: Long): Int

    /**
     * 获取所有未删除分类及其图标信息的响应式数据流。
     *
     * 按排序权重 [CategoryEntity.sortOrder] 升序及 ID 升序排列。
     */
    @Transaction
    @Query("SELECT * FROM tb_category WHERE is_deleted = 0 ORDER BY sort_order ASC, category_id ASC")
    fun getPopulatedCategories(): Flow<List<PopulatedCategory>>

    /**
     * 根据分类类型（支出/收入）获取未删除分类及其图标信息。
     *
     * @param type 分类类型 [CategoryType]
     * @return 匹配类型的聚合分类列表流
     */
    @Transaction
    @Query("SELECT * FROM tb_category WHERE type = :type AND is_deleted = 0 ORDER BY sort_order ASC, category_id ASC")
    fun getPopulatedCategoriesByType(type: CategoryType): Flow<List<PopulatedCategory>>

    /**
     * 观察指定 ID 的未删除分类及其图标信息。
     *
     * @param id 分类 ID
     * @return 聚合分类数据流
     */
    @Transaction
    @Query("SELECT * FROM tb_category WHERE category_id = :id AND is_deleted = 0")
    fun getPopulatedCategoryById(id: Long): Flow<PopulatedCategory?>

    /**
     * 一次性查询未删除的指定分类实体。
     *
     * @param id 分类 ID
     * @return 分类实体，若不存在或已删除则返回 null
     */
    @Query("SELECT * FROM tb_category WHERE category_id = :id AND is_deleted = 0")
    suspend fun getCategoryByIdOnce(id: Long): CategoryEntity?
}

