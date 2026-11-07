package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.model.CardTemplate
import kotlinx.coroutines.flow.Flow

interface CardTemplateRepository {
    fun getTemplatesStream(): Flow<List<CardTemplate>>
    fun getTemplate(id: Long): Flow<CardTemplate?>
    suspend fun createTemplate(template: CardTemplate): Long
    suspend fun deleteTemplate(id: Long)
}

