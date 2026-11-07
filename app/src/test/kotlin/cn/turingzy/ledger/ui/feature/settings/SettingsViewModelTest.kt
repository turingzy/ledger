package cn.turingzy.ledger.ui.feature.settings

import app.cash.turbine.test
import cn.turingzy.ledger.data.repository.CategoryRepository
import cn.turingzy.ledger.data.repository.IconRepository
import cn.turingzy.ledger.model.Category
import cn.turingzy.ledger.model.CategoryType
import cn.turingzy.ledger.model.CategoryWithIcon
import cn.turingzy.ledger.model.Icon
import io.mockk.coEvery
import io.mockk.coVerify
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
    private val iconRepository: IconRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_initialSuccess_combinesCategoriesAndIcons() = runTest {
        val testCategories = listOf(
            CategoryWithIcon(
                category = Category(id = 1L, name = "Food", type = CategoryType.EXPENSE, iconId = 1L),
                icon = Icon(id = 1L, keyName = "ic_food", displayName = "Food", iconUri = "res://"),
            ),
        )
        val testIcons = listOf(
            Icon(id = 1L, keyName = "ic_food", displayName = "Food", iconUri = "res://"),
        )

        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(testCategories)
        coEvery { iconRepository.getIconsStream() } returns flowOf(testIcons)

        viewModel = SettingsViewModel(categoryRepository, iconRepository)

        viewModel.uiState.test {
            assertEquals(SettingsUiState.Loading, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()

            val successItem = awaitItem()
            assertTrue(successItem is SettingsUiState.Success)
            val success = successItem as SettingsUiState.Success
            assertEquals(1, success.categories.size)
            assertEquals("Food", success.categories.first().category.name)
            assertEquals(1, success.availableIcons.size)
        }
    }

    @Test
    fun createCategory_delegatesToRepository() = runTest {
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { iconRepository.getIconsStream() } returns flowOf(emptyList())

        viewModel = SettingsViewModel(categoryRepository, iconRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.createCategory(
            name = "Books",
            type = CategoryType.EXPENSE,
            iconId = 3L,
        )

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            categoryRepository.createCategory(
                match {
                    it.name == "Books" &&
                        it.type == CategoryType.EXPENSE &&
                        it.iconId == 3L &&
                        !it.isSystem
                },
            )
        }
    }

    @Test
    fun deleteCategory_delegatesToRepository() = runTest {
        coEvery { categoryRepository.getCategoriesStream() } returns flowOf(emptyList())
        coEvery { iconRepository.getIconsStream() } returns flowOf(emptyList())

        viewModel = SettingsViewModel(categoryRepository, iconRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.deleteCategory(10L)

        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            categoryRepository.deleteCategory(10L)
        }
    }
}
