package cn.turingzy.ledger.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import cn.turingzy.ledger.database.model.CategoryEntity
import cn.turingzy.ledger.database.model.PopulatedCategory
import cn.turingzy.ledger.model.CategoryType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Query("UPDATE tb_category SET is_deleted = 1 WHERE category_id = :id")
    suspend fun softDeleteCategory(id: Long): Int

    @Transaction
    @Query("SELECT * FROM tb_category WHERE is_deleted = 0 ORDER BY sort_order ASC, category_id ASC")
    fun getPopulatedCategories(): Flow<List<PopulatedCategory>>

    @Transaction
    @Query("SELECT * FROM tb_category WHERE type = :type AND is_deleted = 0 ORDER BY sort_order ASC, category_id ASC")
    fun getPopulatedCategoriesByType(type: CategoryType): Flow<List<PopulatedCategory>>

    @Transaction
    @Query("SELECT * FROM tb_category WHERE category_id = :id AND is_deleted = 0")
    fun getPopulatedCategoryById(id: Long): Flow<PopulatedCategory?>

    @Query("SELECT * FROM tb_category WHERE category_id = :id AND is_deleted = 0")
    suspend fun getCategoryByIdOnce(id: Long): CategoryEntity?
}

