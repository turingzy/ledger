package cn.turingzy.ledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
    onSurfaceVariant = Color(0xFFEBEBF5),
    outline = OutlineDark,
    error = Color(0xFFFF453A),
)

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
    onSurfaceVariant = Color(0xFF636366),
    outline = OutlineLight,
    error = IosRed,
)

@Composable
fun LedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
