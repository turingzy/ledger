package cn.turingzy.ledger.data.repository.settings

import cn.turingzy.ledger.data.database.dao.ParseRuleDao
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.data.database.mapper.asExternalModel
import cn.turingzy.ledger.model.transfer.ParseRule
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * [ParseRuleRepository] 的默认实现。
 *
 * 通过 [ParseRuleDao] 在 IO 调度器上实现解析规则的查询、新增、修改与软删除。
 *
 * @property parseRuleDao 解析规则数据访问对象
 * @property ioDispatcher 用于执行数据库操作的 IO 协程调度器
 */
class DefaultParseRuleRepository @Inject constructor(
    private val parseRuleDao: ParseRuleDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ParseRuleRepository {

    /**
     * 获取所有解析规则列表的流。
     */
    override fun getRulesStream(): Flow<List<ParseRule>> =
        parseRuleDao.getAllRules()
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    /**
     * 根据 ID 获取单个解析规则流。
     *
     * @param id 规则 ID
     */
    override fun getRule(id: Long): Flow<ParseRule?> =
        parseRuleDao.getRuleById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    /**
     * 创建新的解析规则。
     *
     * @param rule 待创建的规则
     * @return 新增规则的自增 ID
     */
    override suspend fun createRule(rule: ParseRule): Long =
        withContext(ioDispatcher) {
            parseRuleDao.insertRule(rule.asEntity())
        }

    /**
     * 更新已有解析规则。
     *
     * @param rule 待更新的规则
     */
    override suspend fun updateRule(rule: ParseRule) {
        withContext(ioDispatcher) {
            parseRuleDao.updateRule(rule.asEntity())
        }
    }

    /**
     * 软删除指定 ID 的解析规则。
     *
     * @param id 待删除的规则 ID
     */
    override suspend fun deleteRule(id: Long) {
        withContext(ioDispatcher) {
            parseRuleDao.softDeleteRule(id)
        }
    }
}

