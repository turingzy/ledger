package cn.turingzy.ledger.ui.feature.ledger

import android.graphics.Bitmap
import android.net.Uri
import cn.turingzy.ledger.data.repository.LedgerCardAppearanceRepository
import cn.turingzy.ledger.model.LedgerCardAppearance
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
class LedgerCardViewModelTest {
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
        val viewModel = LedgerCardViewModel(repository)
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
        val viewModel = LedgerCardViewModel(repository)
        runCurrent()
        viewModel.setTransparencyPercent(80)
        viewModel.setTransparencyEnabled(false)
        assertEquals(80, viewModel.uiState.value.appearance.transparencyPercent)
        assertFalse(viewModel.uiState.value.appearance.transparencyEnabled)
        runCurrent()
        coVerify { repository.setTransparencyPercent(80) }
        coVerify { repository.setTransparencyEnabled(false) }
    }

    @Test
    fun invalidBackgroundFallsBackWithoutBlockingCardSettings() = runTest {
        stored.value = LedgerCardAppearance(backgroundFile = "missing.jpg")
        coEvery { repository.loadBackground(any()) } throws IOException("missing image")
        val viewModel = LedgerCardViewModel(repository)
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
        coEvery { repository.importBackground(uri) } throws IOException("unreadable image")
        val viewModel = LedgerCardViewModel(repository)
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
        val viewModel = LedgerCardViewModel(repository)
        runCurrent()
        viewModel.setTransparencyPercent(80)
        runCurrent()
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun urlImportUsesRepositoryAndRejectsDuplicateSubmissions() = runTest {
        val viewModel = LedgerCardViewModel(repository)
        runCurrent()
        viewModel.importBackgroundUrl("https://example.com/image.jpg")
        viewModel.importBackgroundUrl("https://example.com/other.jpg")
        runCurrent()
        coVerify(exactly = 1) { repository.importBackgroundUrl("https://example.com/image.jpg") }
        coVerify(exactly = 0) { repository.importBackgroundUrl("https://example.com/other.jpg") }
        assertFalse(viewModel.uiState.value.importing)
    }

    @Test
    fun failedUrlImportPreservesCurrentSettingsAndAllowsRetry() = runTest {
        coEvery { repository.importBackgroundUrl(any()) } throws IOException("network unavailable")
        val viewModel = LedgerCardViewModel(repository)
        runCurrent()
        viewModel.importBackgroundUrl("https://example.com/image.jpg")
        runCurrent()
        assertEquals(LedgerCardAppearance(), viewModel.uiState.value.appearance)
        assertNotNull(viewModel.uiState.value.error)
        assertFalse(viewModel.uiState.value.importing)
    }

    @Test
    fun resetBackgroundClearsCustomImageAndRetainsTransparencySettings() = runTest {
        val bitmap = mockk<Bitmap>()
        stored.value = LedgerCardAppearance(
            backgroundFile = "custom.jpg",
            transparencyEnabled = false,
            transparencyPercent = 80,
        )
        coEvery { repository.loadBackground("custom.jpg") } returns bitmap
        coEvery { repository.resetBackground() } answers {
            stored.value = stored.value.copy(backgroundFile = null)
        }
        val viewModel = LedgerCardViewModel(repository)
        runCurrent()
        viewModel.resetBackground()
        runCurrent()
        assertNull(viewModel.uiState.value.background)
        assertNull(viewModel.uiState.value.appearance.backgroundFile)
        assertEquals(80, viewModel.uiState.value.appearance.transparencyPercent)
        assertFalse(viewModel.uiState.value.appearance.transparencyEnabled)
        coVerify(exactly = 1) { repository.resetBackground() }
    }

    @Test
    fun failedBackgroundResetKeepsCurrentImageAndReportsError() = runTest {
        val bitmap = mockk<Bitmap>()
        stored.value = LedgerCardAppearance(backgroundFile = "custom.jpg")
        coEvery { repository.loadBackground("custom.jpg") } returns bitmap
        coEvery { repository.resetBackground() } throws IOException("disk unavailable")
        val viewModel = LedgerCardViewModel(repository)
        runCurrent()
        viewModel.resetBackground()
        runCurrent()
        assertSame(bitmap, viewModel.uiState.value.background)
        assertEquals("custom.jpg", viewModel.uiState.value.appearance.backgroundFile)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test
    fun cancelledSaveDoesNotBecomeUserFacingFailure() = runTest {
        coEvery { repository.setTransparencyEnabled(any()) } throws CancellationException("cancelled")
        val viewModel = LedgerCardViewModel(repository)
        runCurrent()
        viewModel.setTransparencyEnabled(false)
        runCurrent()
        assertNull(viewModel.uiState.value.error)
    }
}
