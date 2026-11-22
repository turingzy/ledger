package cn.turingzy.ledger.ui.component.card.summary

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.turingzy.ledger.data.repository.settings.LedgerCardAppearanceRepository
import cn.turingzy.ledger.model.settings.LedgerCardAppearance
import cn.turingzy.ledger.model.settings.SummaryCardTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * 摘要卡片外观与背景图的 UI 状态。
 *
 * @property appearance 卡片外观属性（包含透明度百分比、关联背景文件名等）。
 * @property background 当前加载完成的背景图片位图；若使用默认背景则为 `null`。
 * @property loading 首次初始化加载中状态。
 * @property importing 背景图片导入与裁切处理中状态。
 * @property error 操作或加载失败时的提示信息。
 */
internal data class SummaryCardUiState(
    val appearance: LedgerCardAppearance = LedgerCardAppearance(),
    val background: Bitmap? = null,
    val loading: Boolean = true,
    val importing: Boolean = false,
    val error: String? = null,
)

/**
 * 摘要卡片外观与背景设置的 ViewModel 基类。
 *
 * 负责监听与维护指定卡片目标 [SummaryCardTarget] 的外观属性、背景图异步加载与图片导入落盘。
 *
 * @property repository 卡片外观配置与背景图片文件仓储。
 * @property target 当前卡片所属的目标类型（如账本首页卡片、资产卡片等）。
 */
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

    /**
     * 设置毛玻璃透明度百分比并异步持久化。
     *
     * @param percent 透明度百分比数值（0 ~ 100）。
     */
    fun setTransparencyPercent(percent: Int) {
        _uiState.update { it.copy(appearance = it.appearance.copy(transparencyPercent = percent)) }
        save { repository.setTransparencyPercent(percent, target) }
    }

    /**
     * 导入用户选中的新背景图片并应用。
     *
     * @param uri 用户从相册等选择的图片 [Uri]。
     */
    fun importBackground(uri: Uri) {
        backgroundOperation {
            val filename = repository.prepareBackground(uri, target)
            try {
                repository.applyPreparedBackground(filename, target)
            } finally {
                withContext(NonCancellable) { repository.discardPreparedBackground(filename) }
            }
        }
    }

    /**
     * 执行背景图相关的异步耗时操作，统一管理导入中状态与异常处理。
     */
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

    /**
     * 清空当前展示的错误信息。
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 异步执行持久化保存，捕获并记录异常。
     */
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
