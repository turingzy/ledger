package cn.turingzy.ledger.ui.feature.bill

import android.graphics.Bitmap
import android.net.Uri
import cn.turingzy.ledger.data.repository.settings.LedgerCardAppearanceRepository
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BillCardViewModelTest {
    private val repository = mockk<LedgerCardAppearanceRepository>(relaxed = true)
    private val stored = MutableStateFlow(LedgerCardAppearance())
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        every { repository.appearance() } returns stored
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun restoredSettingsAndBackgroundAreLoadedWithoutRepeatedImageDecoding() = runTest {
        val bitmap = mockk<Bitmap>()
        stored.value = LedgerCardAppearance(backgroundFile = "background.jpg", transparencyPercent = 75)
        coEvery { repository.loadBackground("background.jpg") } returns bitmap
        val viewModel = BillCardViewModel(repository)
        runCurrent()
        assertFalse(viewModel.uiState.value.loading)
        assertEquals(75, viewModel.uiState.value.appearance.transparencyPercent)
        assertSame(bitmap, viewModel.uiState.value.background)
        stored.value = stored.value.copy(transparencyPercent = 90)
        runCurrent()
        coVerify(exactly = 1) { repository.loadBackground("background.jpg") }
        assertSame(bitmap, viewModel.uiState.value.background)
    }

    @Test
    fun settingChangesAreImmediatelyVisibleAndSaved() = runTest {
        val viewModel = BillCardViewModel(repository)
        runCurrent()
        viewModel.setTransparencyPercent(80)
        assertEquals(80, viewModel.uiState.value.appearance.transparencyPercent)
        runCurrent()
        coVerify { repository.setTransparencyPercent(80) }
    }

    @Test
    fun invalidBackgroundFallsBackWithoutBlockingCardSettings() = runTest {
        stored.value = LedgerCardAppearance(backgroundFile = "missing.jpg")
        coEvery { repository.loadBackground(any()) } throws IOException("missing image")
        val viewModel = BillCardViewModel(repository)
        runCurrent()
        assertFalse(viewModel.uiState.value.loading)
        assertNull(viewModel.uiState.value.background)
        assertNotNull(viewModel.uiState.value.error)
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun failedImportKeepsExistingBackgroundAndReportsError() = runTest {
        val uri = mockk<Uri>()
        coEvery { repository.prepareBackground(uri) } throws IOException("unreadable image")
        val viewModel = BillCardViewModel(repository)
        runCurrent()
        viewModel.importBackground(uri)
        runCurrent()
        assertFalse(viewModel.uiState.value.importing)
        assertEquals(LedgerCardAppearance(), viewModel.uiState.value.appearance)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun failedSaveIsReported() = runTest {
        coEvery { repository.setTransparencyPercent(any()) } throws IOException("disk full")
        val viewModel = BillCardViewModel(repository)
        runCurrent()
        viewModel.setTransparencyPercent(80)
        runCurrent()
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun directImportAppliesOnceAndCleansTheTemporaryImage() = runTest {
        val uri = mockk<Uri>()
        val bitmap = mockk<Bitmap>()
        coEvery { repository.prepareBackground(uri) } returns "unique.jpg"
        coEvery { repository.applyPreparedBackground("unique.jpg") } answers {
            stored.value = stored.value.copy(backgroundFile = "unique.jpg")
        }
        coEvery { repository.loadBackground("unique.jpg") } returns bitmap
        val viewModel = BillCardViewModel(repository)
        runCurrent()
        viewModel.importBackground(uri)
        viewModel.importBackground(uri)
        runCurrent()
        coVerify(exactly = 1) { repository.prepareBackground(uri) }
        coVerify(exactly = 1) { repository.applyPreparedBackground("unique.jpg") }
        coVerify { repository.discardPreparedBackground("unique.jpg") }
        assertSame(bitmap, viewModel.uiState.value.background)
        assertFalse(viewModel.uiState.value.importing)
    }

    @Test
    fun failedApplyCleansTheTemporaryImageAndKeepsTheSavedBackground() = runTest {
        val uri = mockk<Uri>()
        val bitmap = mockk<Bitmap>()
        stored.value = LedgerCardAppearance(backgroundFile = "current.jpg")
        coEvery { repository.loadBackground("current.jpg") } returns bitmap
        coEvery { repository.prepareBackground(uri) } returns "unique.jpg"
        coEvery { repository.applyPreparedBackground("unique.jpg") } throws IOException("disk full")
        val viewModel = BillCardViewModel(repository)
        runCurrent()
        viewModel.importBackground(uri)
        runCurrent()
        coVerify { repository.discardPreparedBackground("unique.jpg") }
        assertSame(bitmap, viewModel.uiState.value.background)
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.importing)
    }

    @Test
    fun cancelledSaveDoesNotBecomeUserFacingFailure() = runTest {
        coEvery { repository.setTransparencyPercent(any()) } throws CancellationException("cancelled")
        val viewModel = BillCardViewModel(repository)
        runCurrent()
        viewModel.setTransparencyPercent(0)
        runCurrent()
        assertNull(viewModel.uiState.value.error)
    }
}
