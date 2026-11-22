package cn.turingzy.ledger.data.repository.category

import cn.turingzy.ledger.model.category.Category
import cn.turingzy.ledger.model.category.CategoryType
import cn.turingzy.ledger.model.category.CategoryWithIcon
import kotlinx.coroutines.flow.Flow

/**
 * 分类数据仓库接口。
 *
 * 提供交易分类（收入、支出等）及其关联图标的查询、创建、更新与软删除等操作。
 */
interface CategoryRepository {
    /**
     * 获取分类列表及其图标的响应式流。
     *
     * @param type 分类类型过滤条件（如支出、收入等）；若为 null 则返回所有分类
     * @return 分类及其图标列表的流
     */
    fun getCategoriesStream(type: CategoryType? = null): Flow<List<CategoryWithIcon>>

    /**
     * 根据分类 ID 获取指定分类及其图标详情的流。
     *
     * @param id 分类 ID
     * @return 匹配的分类详情流；若不存在则发射 null
     */
    fun getCategory(id: Long): Flow<CategoryWithIcon?>

    /**
     * 创建新分类。
     *
     * @param category 待创建的分类信息
     * @return 新建分类的自增 ID
     */
    suspend fun createCategory(category: Category): Long

    /**
     * 全量更新分类信息。
     *
     * @param category 待更新的分类信息
     */
    suspend fun updateCategory(category: Category)

    /**
     * 软删除指定 ID 的分类。
     *
     * @param id 待删除的分类 ID
     */
    suspend fun deleteCategory(id: Long)
}

