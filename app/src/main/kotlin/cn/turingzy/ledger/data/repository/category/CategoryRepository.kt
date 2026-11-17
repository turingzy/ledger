package cn.turingzy.ledger.data.repository.category

import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getCategoriesStream(type: CategoryType? = null): Flow<List<CategoryWithIcon>>
    fun getCategory(id: Long): Flow<CategoryWithIcon?>
    suspend fun createCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(id: Long)
}

