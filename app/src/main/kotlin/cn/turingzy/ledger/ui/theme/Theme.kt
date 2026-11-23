package cn.turingzy.ledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import cn.turingzy.ledger.ui.interaction.rememberUiIndication
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 应用深色模式 Material 3 配色方案。
 */
private val DarkColorScheme = darkColorScheme(
    tertiary = IosGreen,
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1C1C1E),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    outlineVariant = OutlineDark,
    error = Color(0xFFFF453A),
)

/**
 * 应用浅色模式 Material 3 配色方案。
 */
private val LightColorScheme = lightColorScheme(
    tertiary = IosGreen,
    primary = IosBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5EA),
    onPrimaryContainer = IosLabel,
    secondary = IosSecondary,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = IosLabel,
    surface = SurfaceLight,
    onSurface = IosLabel,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = OutlineLight,
    outlineVariant = IosSeparator,
    error = IosRed,
)

/**
 * 应用核心主题提供组件。
 *
 * 应用深色或浅色 Material 3 配色方案与排版规范，并将默认波纹（Ripple）替换为自定义按压反馈 [rememberUiIndication]。
 *
 * @param darkTheme 是否启用深色模式，默认跟随系统
 * @param content 子 Composable 内容插槽
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        val feedback = rememberUiIndication()
        CompositionLocalProvider(LocalIndication provides feedback, LocalRippleConfiguration provides null, content = content)
    }
}

/**
 * 应用主题 Composable 入口（[AppTheme] 的便捷别名）。
 *
 * @param darkTheme 是否启用深色模式，默认跟随系统
 * @param content 子 Composable 内容插槽
 */
@Composable
fun LedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) = AppTheme(darkTheme = darkTheme, content = content)
