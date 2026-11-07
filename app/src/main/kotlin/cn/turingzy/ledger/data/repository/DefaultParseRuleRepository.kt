package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.database.dao.ParseRuleDao
import cn.turingzy.ledger.database.mapper.asEntity
import cn.turingzy.ledger.database.mapper.asExternalModel
import cn.turingzy.ledger.model.ParseRule
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultParseRuleRepository @Inject constructor(
    private val parseRuleDao: ParseRuleDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ParseRuleRepository {

    override fun getRulesStream(): Flow<List<ParseRule>> =
        parseRuleDao.getAllRules()
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override fun getRule(id: Long): Flow<ParseRule?> =
        parseRuleDao.getRuleById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    override suspend fun createRule(rule: ParseRule): Long =
        withContext(ioDispatcher) {
            parseRuleDao.insertRule(rule.asEntity())
        }

    override suspend fun updateRule(rule: ParseRule) {
        withContext(ioDispatcher) {
            parseRuleDao.updateRule(rule.asEntity())
        }
    }

    override suspend fun deleteRule(id: Long) {
        withContext(ioDispatcher) {
            parseRuleDao.softDeleteRule(id)
        }
    }
}

