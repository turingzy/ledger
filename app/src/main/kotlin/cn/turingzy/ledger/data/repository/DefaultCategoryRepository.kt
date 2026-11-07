package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.database.dao.CategoryDao
import cn.turingzy.ledger.database.mapper.asEntity
import cn.turingzy.ledger.database.mapper.asExternalModel
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CategoryRepository {

    override fun getCategoriesStream(type: CategoryType?): Flow<List<CategoryWithIcon>> {
        val flow = if (type == null) {
            categoryDao.getPopulatedCategories()
        } else {
            categoryDao.getPopulatedCategoriesByType(type)
        }
        return flow
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)
    }

    override fun getCategory(id: Long): Flow<CategoryWithIcon?> =
        categoryDao.getPopulatedCategoryById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    override suspend fun createCategory(category: Category): Long =
        withContext(ioDispatcher) {
            categoryDao.insertCategory(category.asEntity())
        }

    override suspend fun updateCategory(category: Category) {
        withContext(ioDispatcher) {
            categoryDao.updateCategory(category.asEntity())
        }
    }

    override suspend fun deleteCategory(id: Long) {
        withContext(ioDispatcher) {
            categoryDao.softDeleteCategory(id)
        }
    }
}

