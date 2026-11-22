package cn.turingzy.ledger.ui

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.view.ContextThemeWrapper
import android.view.Window
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import cn.turingzy.ledger.model.settings.AppPreferences
import cn.turingzy.ledger.ui.component.biz.LocalCurrencyBiz
import cn.turingzy.ledger.ui.theme.LedgerTheme
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 账本应用 Compose 根组件。
 *
 * 负责收集全局偏好设置并配置应用的基础环境：
 * 1. 根据偏好设置覆写应用语言和区域（[Locale]），并提供本地化的 [Context] 和 [Configuration]；
 * 2. 计算深浅色主题模式并同步系统状态栏与导航栏的高亮外观；
 * 3. 在应用处于 [Lifecycle.State.RESUMED] 状态时，定时（每小时）自动刷新汇率；
 * 4. 向下注入本位币 [LocalCurrencyBiz]，并在偏好数据加载完成前展示全屏加载指示器。
 *
 * @param window 当前宿主 Activity 的窗口对象，用于控制系统栏外观样式。
 * @param viewModel 全局应用状态 ViewModel，默认通过 Hilt 获取。
 */
@Composable
internal fun LedgerRoot(window: Window, viewModel: AppStateViewModel = hiltViewModel()) {
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val settings = preferences ?: AppPreferences()
    val context = LocalContext.current
    val systemConfiguration = LocalConfiguration.current
    val configuration = remember(settings.language, systemConfiguration) {
        Configuration(systemConfiguration).apply {
            if (settings.language != "system") setLocales(LocaleList(Locale.forLanguageTag(settings.language)))
        }
    }
    val localizedContext = remember(context, configuration) { localizedActivityContext(context, configuration) }
    val dark = when (settings.theme) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val systemBars = remember(window) { WindowCompat.getInsetsController(window, window.decorView) }
    SideEffect {
        systemBars.isAppearanceLightStatusBars = !dark
        systemBars.isAppearanceLightNavigationBars = !dark
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, preferences?.primaryCurrency) {
        if (preferences == null) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                viewModel.refreshRates()
                delay((60 * 60 * 1000L).milliseconds)
            }
        }
    }
    CompositionLocalProvider(LocalContext provides localizedContext, LocalConfiguration provides configuration,
        LocalCurrencyBiz provides settings.primaryCurrency) {
        LedgerTheme(darkTheme = dark) {
            if (preferences == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            else LedgerApp()
        }
    }
}

/**
 * 创建应用了指定语言和区域配置的包装 Context。
 *
 * 通过 [ContextThemeWrapper] 覆写当前配置，确保在不重启 Activity 的情况下动态生效语言偏好。
 *
 * @param context 当前上下文。
 * @param configuration 包含了目标语言区域的配置对象。
 * @return 覆写了指定配置的 [ContextThemeWrapper]。
 */
internal fun localizedActivityContext(context: Context, configuration: Configuration): ContextThemeWrapper =
    ContextThemeWrapper(context, context.theme).apply { applyOverrideConfiguration(configuration) }

