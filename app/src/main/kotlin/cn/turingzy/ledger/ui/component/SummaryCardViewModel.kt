package cn.turingzy.ledger.ui.component

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.LedgerCardAppearanceRepository
import cn.turingzy.ledger.model.LedgerCardAppearance
import cn.turingzy.ledger.model.SummaryCardTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

internal data class SummaryCardUiState(
    val appearance: LedgerCardAppearance = LedgerCardAppearance(),
    val background: Bitmap? = null,
    val loading: Boolean = true,
    val importing: Boolean = false,
    val error: String? = null,
    val preparedBackground: String? = null,
)

internal open class SummaryCardViewModel(
    private val repository: LedgerCardAppearanceRepository,
    private val target: SummaryCardTarget,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SummaryCardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.appearance(target).collectLatest { appearance ->
                try {
                    val current = _uiState.value
                    val background = when {
                        appearance.backgroundFile == null -> null
                        appearance.backgroundFile == current.appearance.backgroundFile -> current.background
                        else -> repository.loadBackground(appearance.backgroundFile)
                    }
                    _uiState.update { it.copy(appearance = appearance, background = background, loading = false) }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    Timber.e(error, "Unable to load card background")
                    _uiState.update { it.copy(appearance = appearance, background = null, loading = false, error = "背景图加载失败，已显示默认背景") }
                }
            }
        }
    }

    fun setTransparencyPercent(percent: Int) {
        _uiState.update { it.copy(appearance = it.appearance.copy(transparencyPercent = percent)) }
        save { repository.setTransparencyPercent(percent, target) }
    }

    fun resetBackground() {
        applyBackground(restoreDefault = true)
    }

    fun prepareBackground(uri: Uri) {
        backgroundOperation {
            val filename = repository.prepareBackground(uri, target)
            val previous = _uiState.value.preparedBackground
            _uiState.update { it.copy(preparedBackground = filename) }
            previous?.let { repository.discardPreparedBackground(it) }
        }
    }

    fun discardPreparedBackground() {
        val filename = _uiState.value.preparedBackground ?: return
        _uiState.update { it.copy(preparedBackground = null) }
        save { repository.discardPreparedBackground(filename) }
    }

    fun applyBackground(restoreDefault: Boolean, onSuccess: () -> Unit = {}) {
        val filename = _uiState.value.preparedBackground
        if (!restoreDefault && filename == null) return
        backgroundOperation {
            if (restoreDefault) {
                repository.resetBackground(target)
                filename?.let { repository.discardPreparedBackground(it) }
            } else if (filename != null) {
                repository.applyPreparedBackground(filename, target)
            }
            _uiState.update { it.copy(preparedBackground = null) }
            onSuccess()
        }
    }

    private fun backgroundOperation(action: suspend () -> Unit) {
        if (_uiState.value.importing) return
        _uiState.update { it.copy(importing = true, error = null) }
        viewModelScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.e(error, "Unable to update card background")
                _uiState.update { it.copy(error = "背景图片处理失败，请重试") }
            } finally {
                _uiState.update { it.copy(importing = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun save(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.e(error, "Unable to save card appearance")
                _uiState.update { it.copy(error = "卡片设置保存失败，请重试") }
            }
        }
    }
}
