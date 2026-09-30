package io.github.twatanabe1436.sodateru.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 小麦色 (primary) と若葉色 (secondary)
private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5A2B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCBE),
    onPrimaryContainer = Color(0xFF2E1500),
    secondary = Color(0xFF55682E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8EDAA),
    onSecondaryContainer = Color(0xFF161F00),
    tertiary = Color(0xFF38656A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFBCEBF0),
    onTertiaryContainer = Color(0xFF002023),
    background = Color(0xFFFFF8F1),
    onBackground = Color(0xFF211A14),
    surface = Color(0xFFFFF8F1),
    onSurface = Color(0xFF211A14),
    surfaceVariant = Color(0xFFF2E0D0),
    onSurfaceVariant = Color(0xFF51443A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCF2E9),
    surfaceContainer = Color(0xFFF7ECE3),
    surfaceContainerHigh = Color(0xFFF1E6DD),
    surfaceContainerHighest = Color(0xFFEBE1D8),
    outline = Color(0xFF847468),
    outlineVariant = Color(0xFFD6C3B4),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB871),
    onPrimary = Color(0xFF4B2800),
    primaryContainer = Color(0xFF6C4316),
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = Color(0xFFBCD190),
    onSecondary = Color(0xFF293505),
    secondaryContainer = Color(0xFF3E4F19),
    onSecondaryContainer = Color(0xFFD8EDAA),
    tertiary = Color(0xFFA0CFD4),
    onTertiary = Color(0xFF00363B),
    tertiaryContainer = Color(0xFF1E4D52),
    onTertiaryContainer = Color(0xFFBCEBF0),
    background = Color(0xFF19120C),
    onBackground = Color(0xFFEFE0D5),
    surface = Color(0xFF19120C),
    onSurface = Color(0xFFEFE0D5),
    surfaceVariant = Color(0xFF51443A),
    onSurfaceVariant = Color(0xFFD6C3B4),
    surfaceContainerLowest = Color(0xFF130D08),
    surfaceContainerLow = Color(0xFF211A14),
    surfaceContainer = Color(0xFF261E18),
    surfaceContainerHigh = Color(0xFF312822),
    surfaceContainerHighest = Color(0xFF3C332C),
    outline = Color(0xFF9F8D80),
    outlineVariant = Color(0xFF51443A),
    error = Color(0xFFFFB4AB),
)

/** 評価 (★) の色。 */
val StarColor = Color(0xFFE8A317)

@Composable
fun SodateruTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
