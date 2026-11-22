package cn.turingzy.ledger.data.repository.category

import cn.turingzy.ledger.model.category.Icon
import kotlinx.coroutines.flow.Flow

/**
 * 图标数据仓库接口。
 *
 * 提供分类图标的查询、创建与软删除操作。
 */
interface IconRepository {
    /**
     * 获取所有可用图标列表的响应式流。
     */
    fun getIconsStream(): Flow<List<Icon>>

    /**
     * 根据图标 ID 获取指定图标流。
     *
     * @param id 图标 ID
     * @return 匹配的图标流；若不存在则发射 null
     */
    fun getIcon(id: Long): Flow<Icon?>

    /**
     * 创建新图标。
     *
     * @param icon 待创建的图标对象
     * @return 新增图标的自增 ID
     */
    suspend fun createIcon(icon: Icon): Long

    /**
     * 软删除指定 ID 的图标。
     *
     * @param id 待删除的图标 ID
     */
    suspend fun deleteIcon(id: Long)
}

