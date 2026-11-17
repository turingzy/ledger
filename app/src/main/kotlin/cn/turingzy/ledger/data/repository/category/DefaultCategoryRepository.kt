package cn.turingzy.ledger.data.repository.category

import cn.turingzy.ledger.data.database.dao.CategoryDao
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.data.database.mapper.asExternalModel
import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
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

