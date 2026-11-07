package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.model.ParseRule
import kotlinx.coroutines.flow.Flow

interface ParseRuleRepository {
    fun getRulesStream(): Flow<List<ParseRule>>
    fun getRule(id: Long): Flow<ParseRule?>
    suspend fun createRule(rule: ParseRule): Long
    suspend fun updateRule(rule: ParseRule)
    suspend fun deleteRule(id: Long)
}

