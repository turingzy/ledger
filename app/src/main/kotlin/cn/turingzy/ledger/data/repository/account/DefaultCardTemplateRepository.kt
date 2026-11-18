package cn.turingzy.ledger.data.repository.account

import cn.turingzy.ledger.data.database.dao.CardTemplateDao
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.data.database.mapper.asExternalModel
import cn.turingzy.ledger.data.database.seed.ensureBuiltInCardFaces
import cn.turingzy.ledger.model.account.BuiltInCardFace
import cn.turingzy.ledger.model.account.CardTemplate
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultCardTemplateRepository @Inject constructor(
    private val cardTemplateDao: CardTemplateDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CardTemplateRepository {

    override fun getTemplatesStream(): Flow<List<CardTemplate>> =
        cardTemplateDao.getAllTemplates()
            .onStart { cardTemplateDao.ensureBuiltInCardFaces() }
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override fun getTemplate(id: Long): Flow<CardTemplate?> =
        cardTemplateDao.getTemplateById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    override suspend fun createTemplate(template: CardTemplate): Long =
        withContext(ioDispatcher) {
            require(BuiltInCardFace.byId(template.id) == null)
            cardTemplateDao.insertTemplate(template.asEntity())
        }

    override suspend fun deleteTemplate(id: Long) {
        withContext(ioDispatcher) {
            if (BuiltInCardFace.byId(id) == null) cardTemplateDao.softDeleteTemplate(id)
        }
    }
}

