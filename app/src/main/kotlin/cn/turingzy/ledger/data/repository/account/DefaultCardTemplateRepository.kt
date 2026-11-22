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

/**
 * [CardTemplateRepository] 的默认实现。
 *
 * 封装卡片模板在 Room 数据库中的增删查操作，并在初次加载流时确保内置卡面数据已植入。
 *
 * @property cardTemplateDao 卡片模板数据访问对象
 * @property ioDispatcher 用于执行数据库操作的 IO 协程调度器
 */
class DefaultCardTemplateRepository @Inject constructor(
    private val cardTemplateDao: CardTemplateDao,
    @param:Dispatcher(LedgerDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CardTemplateRepository {

    /**
     * 获取所有卡片模板流。
     *
     * 在流开始时自动确保内置卡面存在，并在 IO 调度器上转换为领域模型。
     */
    override fun getTemplatesStream(): Flow<List<CardTemplate>> =
        cardTemplateDao.getAllTemplates()
            .onStart { cardTemplateDao.ensureBuiltInCardFaces() }
            .map { list -> list.map { it.asExternalModel() } }
            .flowOn(ioDispatcher)

    /**
     * 根据 ID 获取单个卡片模板流。
     *
     * @param id 模板 ID
     */
    override fun getTemplate(id: Long): Flow<CardTemplate?> =
        cardTemplateDao.getTemplateById(id)
            .map { it?.asExternalModel() }
            .flowOn(ioDispatcher)

    /**
     * 创建自定义卡片模板。
     *
     * 校验模板 ID 不能与内置卡面 ID 冲突。
     *
     * @param template 待创建的卡片模板
     * @return 新增模板的自增 ID
     * @throws IllegalArgumentException 当模板 ID 与内置卡面 ID 冲突时抛出
     */
    override suspend fun createTemplate(template: CardTemplate): Long =
        withContext(ioDispatcher) {
            require(BuiltInCardFace.byId(template.id) == null)
            cardTemplateDao.insertTemplate(template.asEntity())
        }

    /**
     * 软删除指定 ID 的卡片模板。
     *
     * @param id 待删除的模板 ID
     */
    override suspend fun deleteTemplate(id: Long) {
        withContext(ioDispatcher) {
            cardTemplateDao.softDeleteTemplate(id)
        }
    }
}

