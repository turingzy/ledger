package cn.turingzy.ledger.data.repository.settings

import cn.turingzy.ledger.model.transfer.ParseRule
import kotlinx.coroutines.flow.Flow

/**
 * 账单导入解析规则数据仓库接口。
 *
 * 提供账单导入规则（如微信、支付宝或自定义规则）的查询、创建、修改及软删除。
 */
interface ParseRuleRepository {
    /**
     * 获取所有导入解析规则列表的响应式流。
     */
    fun getRulesStream(): Flow<List<ParseRule>>

    /**
     * 根据规则 ID 获取指定解析规则流。
     *
     * @param id 规则 ID
     * @return 匹配的规则对象流；若不存在则发射 null
     */
    fun getRule(id: Long): Flow<ParseRule?>

    /**
     * 创建新的解析规则。
     *
     * @param rule 待创建的规则对象
     * @return 新增规则的自增 ID
     */
    suspend fun createRule(rule: ParseRule): Long

    /**
     * 更新已有解析规则。
     *
     * @param rule 待更新的规则对象
     */
    suspend fun updateRule(rule: ParseRule)

    /**
     * 软删除指定 ID 的解析规则。
     *
     * @param id 待删除的规则 ID
     */
    suspend fun deleteRule(id: Long)
}

