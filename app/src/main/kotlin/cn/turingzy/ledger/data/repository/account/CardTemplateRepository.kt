package cn.turingzy.ledger.data.repository.account

import cn.turingzy.ledger.model.account.CardTemplate
import kotlinx.coroutines.flow.Flow

/**
 * 卡片模板数据仓库接口。
 *
 * 提供卡片外观模板的查询、创建与软删除操作。
 */
interface CardTemplateRepository {
    /**
     * 获取所有可用的卡片模板列表流。
     *
     * 包含内置卡面和自定义卡面模板。
     */
    fun getTemplatesStream(): Flow<List<CardTemplate>>

    /**
     * 根据模板 ID 获取指定的卡片模板流。
     *
     * @param id 模板 ID
     * @return 匹配的卡片模板流；若未找到则发射 null
     */
    fun getTemplate(id: Long): Flow<CardTemplate?>

    /**
     * 创建自定义卡片模板。
     *
     * @param template 待创建的卡片模板
     * @return 新建模板的自增 ID
     */
    suspend fun createTemplate(template: CardTemplate): Long

    /**
     * 软删除指定 ID 的卡片模板。
     *
     * @param id 待删除的模板 ID
     */
    suspend fun deleteTemplate(id: Long)
}

