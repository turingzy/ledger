package cn.turingzy.ledger.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.account.AccountRepository
import cn.turingzy.ledger.data.repository.account.CardTemplateRepository
import cn.turingzy.ledger.data.repository.category.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import android.net.Uri
import cn.turingzy.ledger.BuildConfig
import cn.turingzy.ledger.data.repository.settings.ParseRuleRepository
import cn.turingzy.ledger.data.repository.settings.SettingsDataRepository
import cn.turingzy.ledger.model.transfer.BillParseRule
import cn.turingzy.ledger.model.transfer.ParseRule
import cn.turingzy.ledger.model.account.CardTemplate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * 设置界面的 ViewModel。
 *
 * 负责聚合和管理设置主页及各子页面的数据，包括分类列表、卡片模板、关联账户、
 * 账单解析规则、版本更新检测以及账单导入导出等核心业务逻辑。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val cardTemplateRepository: CardTemplateRepository,
    private val accountRepository: AccountRepository,
    private val ruleRepository: ParseRuleRepository,
    private val dataRepository: SettingsDataRepository,
    private val updates: cn.turingzy.ledger.data.repository.settings.AppUpdateRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    /**
     * 设置界面的只读 UI 状态流。
     *
     * 汇集分类、卡片模板、账户、解析规则及错误提示消息并响应式发射。
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        categoryRepository.getCategoriesStream(),
        cardTemplateRepository.getTemplatesStream(),
        accountRepository.getAccountsStream(),
        ruleRepository.getRulesStream(),
        message,
    ) { categories, templates, accounts, rules, errorMessage ->
        SettingsUiState.Success(
            appVersion = BuildConfig.VERSION_NAME,
            message = errorMessage,
            rules = rules.map { rule ->
                Json.decodeFromString<BillParseRule>(rule.mappingConfig).copy(
                    id = rule.id, name = rule.name, startRow = rule.startRow.toString(),
                    accountId = rule.defaultAccountId, categoryId = rule.defaultCategoryId,
                )
            },
            categories = categories,
            cardTemplates = templates,
            accounts = accounts,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState.Loading,
    )
    private val templateJobs = mutableMapOf<Long, Job>()

    init { viewModelScope.launch { attempt { dataRepository.ensureDefaultRules() } } }

    /**
     * 检查是否有最新的应用版本发布。
     *
     * @return 最新版本号字符串，若无需更新或获取失败则返回 null
     */
    suspend fun checkLatest(): String? = updates.latestVersion()

    /**
     * 关闭当前显示的提示或错误弹窗。
     */
    fun dismissMessage() { message.value = null }

    /**
     * 执行挂起操作并统一捕获非取消异常，将错误信息记录到日志并更新到 [message] 中通知 UI。
     *
     * @param block 要执行的挂起代码块
     * @return 执行成功返回结果，失败返回 null
     */
    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Timber.e(error, "Settings action failed")
        message.value = error.message ?: "操作失败，请重试"
        null
    }

    private val ruleWrites = Mutex()

    /**
     * 保存或更新账单解析规则。
     *
     * 若规则已存在则执行更新，否则以新 ID 创建规则。
     *
     * @param rule 待保存的账单解析规则
     */
    fun saveRule(rule: BillParseRule) {
        viewModelScope.launch {
            ruleWrites.withLock {
                attempt {
                    require(rule.valid)
                    val model = ParseRule(id = rule.id, name = rule.name, startRow = rule.startRow.toInt(),
                        mappingConfig = Json.encodeToString(rule), defaultAccountId = rule.accountId, defaultCategoryId = rule.categoryId)
                    val existing = (uiState.value as? SettingsUiState.Success)?.rules.orEmpty().any { it.id == rule.id }
                    if (existing) ruleRepository.updateRule(model) else ruleRepository.createRule(model.copy(id = 0))
                }
            }
        }
    }

    /**
     * 根据规则 ID 删除指定的账单解析规则。
     *
     * @param id 规则唯一标识 ID
     */
    fun deleteRule(id: Long) {
        viewModelScope.launch { ruleWrites.withLock { attempt { ruleRepository.deleteRule(id) } } }
    }

    /**
     * 根据模板 ID 删除指定的卡片背景模板，同时取消该模板未完成的延迟保存任务。
     *
     * @param id 模板唯一标识 ID
     */
    fun deleteTemplate(id: Long) {
        templateJobs.remove(id)?.cancel()
        viewModelScope.launch { attempt { cardTemplateRepository.deleteTemplate(id) } }
    }

    /**
     * 更新卡片模板配置，内置 300ms 防抖机制以避免用户频繁拖动或选择时频繁写入数据库。
     *
     * @param template 待更新的卡片模板实体
     */
    fun updateTemplate(template: CardTemplate) {
        templateJobs.remove(template.id)?.cancel()
        templateJobs[template.id] = viewModelScope.launch {
            delay(300)
            attempt { dataRepository.saveTemplate(template) }
        }
    }

    /**
     * 创建新的卡片模板并持久化到本地。
     *
     * @param template 新建的卡片模板实体
     * @return 创建成功返回生成的模板 ID，失败返回 null
     */
    suspend fun createTemplate(template: CardTemplate): Long? = attempt { dataRepository.saveTemplate(template.copy(id = 0)) }

    /**
     * 将外部选中的图片 Uri 复制到应用内部私有存储目录。
     *
     * @param uri 外部图片 Uri
     * @return 复制成功后的本地图片相对或绝对路径，失败返回 null
     */
    suspend fun copyImage(uri: Uri): String? = attempt { dataRepository.copyImage(uri) }

    /**
     * 预解析并预览待导入的账单文件。
     *
     * @param uri 账单文件 Uri
     * @param rule 所使用的账单解析规则
     * @param accountId 导入的目标账户 ID（可选）
     * @return 导入预览结果，包括可导入账单项、可能重复账单项和错误信息
     */
    suspend fun previewImport(uri: Uri, rule: BillParseRule, accountId: Long?) = dataRepository.previewImport(uri, rule, accountId)

    /**
     * 执行账单正式导入流程。
     *
     * @param uri 账单文件 Uri
     * @param rule 账单解析规则
     * @param accountId 目标账户 ID
     * @param includePossibleDuplicates 是否包含疑似重复的账单
     * @return 实际成功导入的账单条目数
     */
    suspend fun importBills(uri: Uri, rule: BillParseRule, accountId: Long?, includePossibleDuplicates: Boolean) = dataRepository.importBills(uri, rule, accountId, includePossibleDuplicates)

    /**
     * 导出指定时间范围内的账单数据至外部存储文件。
     *
     * @param uri 导出的目标文件 Uri
     * @param excel 为 true 导出为 Excel 格式，为 false 导出为 CSV 格式
     * @param start 导出起止时间的时间戳（毫秒）
     * @param end 导出截止时间的时间戳（毫秒）
     * @return 成功导出的账单条目数量
     */
    suspend fun exportBills(uri: Uri, excel: Boolean, start: Long, end: Long): Int = dataRepository.exportBills(uri, excel, start, end)
}
