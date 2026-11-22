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

/**
 * [CategoryRepository] 的默认实现。
 *
 * 通过 [CategoryDao] 在 IO 调度器上实现分类数据的查询、级联图标装载与持久化。
 *
 * @property categoryDao 分类数据访问对象
 * @property ioDispatcher 用于执行数据库操作的 IO 协程调度器
 */
class DefaultCategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CategoryRepository {

    /**
     * 获取指定类型的分类列表及其关联图标流。
     *
     * @param type 分类类型；若为 null 则查询全部类型
     */
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

    /**
     * 根据 ID 获取包含关联图标的分类详情流。
     *
     * @param id 分类 ID
     */
    override fun getCategory(id: Long): Flow<CategoryWithIcon?> =
        categoryDao.getPopulatedCategoryById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    /**
     * 创建新分类。
     *
     * @param category 待创建的分类
     * @return 新建分类自增 ID
     */
    override suspend fun createCategory(category: Category): Long =
        withContext(ioDispatcher) {
            categoryDao.insertCategory(category.asEntity())
        }

    /**
     * 更新分类信息。
     *
     * @param category 待更新的分类
     */
    override suspend fun updateCategory(category: Category) {
        withContext(ioDispatcher) {
            categoryDao.updateCategory(category.asEntity())
        }
    }

    /**
     * 软删除指定 ID 的分类。
     *
     * @param id 待删除的分类 ID
     */
    override suspend fun deleteCategory(id: Long) {
        withContext(ioDispatcher) {
            categoryDao.softDeleteCategory(id)
        }
    }
}

