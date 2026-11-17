package cn.turingzy.ledger.data.repository.account

import cn.turingzy.ledger.model.account.CardTemplate
import kotlinx.coroutines.flow.Flow

interface CardTemplateRepository {
    fun getTemplatesStream(): Flow<List<CardTemplate>>
    fun getTemplate(id: Long): Flow<CardTemplate?>
    suspend fun createTemplate(template: CardTemplate): Long
    suspend fun deleteTemplate(id: Long)
}

