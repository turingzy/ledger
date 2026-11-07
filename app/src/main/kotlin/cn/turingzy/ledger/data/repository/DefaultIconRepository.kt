package cn.turingzy.ledger.data.repository

import cn.turingzy.ledger.database.dao.IconDao
import cn.turingzy.ledger.database.mapper.asEntity
import cn.turingzy.ledger.database.mapper.asExternalModel
import cn.turingzy.ledger.model.Icon
import cn.turingzy.ledger.util.Dispatcher
import cn.turingzy.ledger.util.LedgerDispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultIconRepository @Inject constructor(
    private val iconDao: IconDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : IconRepository {

    override fun getIconsStream(): Flow<List<Icon>> =
        iconDao.getAllIcons()
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    override fun getIcon(id: Long): Flow<Icon?> =
        iconDao.getIconById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    override suspend fun createIcon(icon: Icon): Long =
        withContext(ioDispatcher) {
            iconDao.insertIcon(icon.asEntity())
        }

    override suspend fun deleteIcon(id: Long) {
        withContext(ioDispatcher) {
            iconDao.softDeleteIcon(id)
        }
    }
}

