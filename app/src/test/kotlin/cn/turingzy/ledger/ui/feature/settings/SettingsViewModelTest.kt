package cn.turingzy.ledger.ui.feature.settings

import app.cash.turbine.test
import cn.turingzy.ledger.data.repository.AccountRepository
import cn.turingzy.ledger.data.repository.CardTemplateRepository
import cn.turingzy.ledger.model.CardTemplate
import cn.turingzy.ledger.data.repository.CategoryRepository
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val categoryRepository: CategoryRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk()
    private val cardTemplateRepository: CardTemplateRepository = mockk()
    private val rules: cn.turingzy.ledger.data.repository.ParseRuleRepository = mockk()
    private val data: cn.turingzy.ledger.data.repository.SettingsDataRepository = mockk(relaxed = true)
    private val updates: cn.turingzy.ledger.data.repository.AppUpdateRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { rules.getRulesStream() } returns flowOf(emptyList())
        coEvery { accountRepository.getAccountsStream() } returns flowOf(emptyList())
        coEvery { cardTemplateRepository.getTemplatesStream() } returns flowOf(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initialSuccess_combinesCategoriesTemplatesAndAccounts() = runTest {
        val testCategories = listOf(
            CategoryWithIcon(
                category = Category(id = 1L, name = "Food", type = CategoryType.EXPENSE, iconId = 1L),
                icon = Icon(id = 1L, keyName = "ic_food", displayName = "Food", iconUri = "res://"),
            ),
        )
        val templates = listOf(CardTemplate(id = 2L, name = "商务黑金", coverUri = "res://drawable/card_black_gold", isSystem = true))
        coEvery { cardTemplateRepository.getTemplatesStream() } returns flowOf(templates)
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(testCategories)

        viewModel = SettingsViewModel(categoryRepository, cardTemplateRepository, accountRepository, rules, data, updates)

        viewModel.uiState.test {
            assertEquals(SettingsUiState.Loading, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()

            val successItem = awaitItem()
            assertTrue(successItem is SettingsUiState.Success)
            val success = successItem as SettingsUiState.Success
            assertEquals(1, success.categories.size)
            assertEquals("Food", success.categories.first().category.name)
            assertEquals(templates, success.cardTemplates)
        }
    }

    @Test
    fun rulesAreLoadedFromRepositoryAndNewRulesUseGeneratedIds() = runTest {
        val rule = cn.turingzy.ledger.model.BillParseRule.defaults().first().copy(id = 7)
        val stored = cn.turingzy.ledger.model.ParseRule(id = 7, name = rule.name, startRow = 2, mappingConfig = Json.encodeToString(rule))
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { rules.getRulesStream() } returns flowOf(listOf(stored))
        coEvery { rules.createRule(any()) } returns 8L
        coEvery { rules.updateRule(any()) } returns Unit
        viewModel = SettingsViewModel(categoryRepository, cardTemplateRepository, accountRepository, rules, data, updates)
        viewModel.uiState.test {
            awaitItem()
            testDispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem() as SettingsUiState.Success
            assertEquals(listOf(rule), state.rules)
            viewModel.saveRule(rule.copy(name = "修改后的规则"))
            viewModel.saveRule(rule.copy(id = 99, name = "新规则"))
            testDispatcher.scheduler.advanceUntilIdle()
            coVerify { rules.updateRule(match { it.id == 7L && it.name == "修改后的规则" }) }
            coVerify { rules.createRule(match { it.id == 0L && it.name == "新规则" }) }
        }
    }

    @Test
    fun deletingTemplateCancelsPendingEdits() = runTest {
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { cardTemplateRepository.deleteTemplate(any()) } returns Unit
        viewModel = SettingsViewModel(categoryRepository, cardTemplateRepository, accountRepository, rules, data, updates)
        viewModel.updateTemplate(CardTemplate(1, "卡面", "res://cover"))
        viewModel.deleteTemplate(1)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { data.saveTemplate(any()) }
        coVerify(exactly = 1) { cardTemplateRepository.deleteTemplate(1) }
    }

}
