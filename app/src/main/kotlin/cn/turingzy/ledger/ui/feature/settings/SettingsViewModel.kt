package cn.turingzy.ledger.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.data.repository.CardTemplateRepository
import cn.turingzy.ledger.data.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import android.net.Uri
import cn.turingzy.ledger.BuildConfig
import cn.turingzy.ledger.data.repository.ParseRuleRepository
import cn.turingzy.ledger.data.repository.SettingsDataRepository
import cn.turingzy.ledger.model.BillParseRule
import cn.turingzy.ledger.model.ParseRule
import cn.turingzy.ledger.model.CardTemplate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val cardTemplateRepository: CardTemplateRepository,
    private val accountRepository: AccountRepository,
    private val ruleRepository: ParseRuleRepository,
    private val dataRepository: SettingsDataRepository,
    private val updates: cn.turingzy.ledger.data.repository.AppUpdateRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)
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

    suspend fun checkLatest(): String? = updates.latestVersion()

    fun dismissMessage() { message.value = null }

    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Timber.e(error, "Settings action failed")
        message.value = error.message ?: "操作失败，请重试"
        null
    }

    fun saveRule(rule: BillParseRule) {
        viewModelScope.launch {
            attempt {
                require(rule.valid)
                val model = ParseRule(id = rule.id, name = rule.name, startRow = rule.startRow.toInt(),
                    mappingConfig = Json.encodeToString(rule), defaultAccountId = rule.accountId, defaultCategoryId = rule.categoryId)
                val existing = (uiState.value as? SettingsUiState.Success)?.rules.orEmpty().any { it.id == rule.id }
                if (existing) ruleRepository.updateRule(model) else ruleRepository.createRule(model.copy(id = 0))
            }
        }
    }

    fun deleteRule(id: Long) { viewModelScope.launch { attempt { ruleRepository.deleteRule(id) } } }
    fun deleteTemplate(id: Long) {
        templateJobs.remove(id)?.cancel()
        viewModelScope.launch { attempt { cardTemplateRepository.deleteTemplate(id) } }
    }
    fun updateTemplate(template: CardTemplate) {
        templateJobs.remove(template.id)?.cancel()
        templateJobs[template.id] = viewModelScope.launch {
            delay(300)
            attempt { dataRepository.saveTemplate(template) }
        }
    }
    suspend fun createTemplate(template: CardTemplate): Long? = attempt { dataRepository.saveTemplate(template.copy(id = 0)) }
    suspend fun copyImage(uri: Uri): String? = attempt { dataRepository.copyImage(uri) }
    suspend fun importBills(uri: Uri, rule: BillParseRule, accountId: Long?): Int = dataRepository.importBills(uri, rule, accountId)
    suspend fun exportBills(uri: Uri, excel: Boolean, start: Long, end: Long): Int = dataRepository.exportBills(uri, excel, start, end)
    suspend fun backup(uri: Uri, bills: Boolean, settings: Boolean) {
        templateJobs.values.toList().joinAll()
        dataRepository.backup(uri, bills, settings)
    }
    suspend fun restore(uri: Uri) {
        templateJobs.values.toList().forEach { it.cancelAndJoin() }
        templateJobs.clear()
        dataRepository.restore(uri)
    }
}
