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
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
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

        viewModel = SettingsViewModel(categoryRepository, cardTemplateRepository, accountRepository)

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

}
