package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.database.dao.CardTemplateDao
import cn.turingzy.ledger.database.mapper.asEntity
import cn.turingzy.ledger.database.mapper.asExternalModel
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultCardTemplateRepository @Inject constructor(
    private val cardTemplateDao: CardTemplateDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CardTemplateRepository {

    override fun getTemplatesStream(): Flow<List<CardTemplate>> =
        cardTemplateDao.getAllTemplates()
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override fun getTemplate(id: Long): Flow<CardTemplate?> =
        cardTemplateDao.getTemplateById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    override suspend fun createTemplate(template: CardTemplate): Long =
        withContext(ioDispatcher) {
            cardTemplateDao.insertTemplate(template.asEntity())
        }

    override suspend fun deleteTemplate(id: Long) {
        withContext(ioDispatcher) {
            cardTemplateDao.softDeleteTemplate(id)
        }
    }
}

