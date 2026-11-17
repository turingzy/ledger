package cn.turingzy.ledger.data.repository.category

import cn.turingzy.ledger.data.database.dao.IconDao
import cn.turingzy.ledger.data.database.mapper.asEntity
import cn.turingzy.ledger.data.database.mapper.asExternalModel
import cn.turingzy.ledger.model.category.Icon
import cn.turingzy.ledger.di.Dispatcher
import cn.turingzy.ledger.di.LedgerDispatchers
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

