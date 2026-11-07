package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategoriesStream(type: CategoryType? = null): Flow<List<CategoryWithIcon>>
    fun getCategory(id: Long): Flow<CategoryWithIcon?>
    suspend fun createCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: Long)
}

