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

internal fun localizedActivityContext(context: Context, configuration: Configuration): ContextThemeWrapper =
    ContextThemeWrapper(context, context.theme).apply { applyOverrideConfiguration(configuration) }

