package io.github.twatanabe1436.hanaso.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.github.twatanabe1436.hanaso.core.Rating

// パープルのブランドカラー。端末の壁紙色 (Material You) には合わせず、どの端末でも同じ見た目にする。
private val LightColors = lightColorScheme(
    primary = Color(0xFF5B4CF5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF170065),
    inversePrimary = Color(0xFFC6BFFF),
    secondary = Color(0xFF5E5C71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4E0F9),
    onSecondaryContainer = Color(0xFF1B1A2C),
    tertiary = Color(0xFF9A4600),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDBC8),
    onTertiaryContainer = Color(0xFF321300),
    background = Color(0xFFF8F6FF),
    onBackground = Color(0xFF1C1B22),
    surface = Color(0xFFF8F6FF),
    onSurface = Color(0xFF1C1B22),
    surfaceVariant = Color(0xFFE5E1EC),
    onSurfaceVariant = Color(0xFF47464F),
    surfaceTint = Color(0xFF5B4CF5),
    inverseSurface = Color(0xFF312F38),
    inverseOnSurface = Color(0xFFF3EFFA),
    outline = Color(0xFF787680),
    outlineVariant = Color(0xFFC9C5D0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFF8F6FF),
    surfaceDim = Color(0xFFDDD9E4),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F0FB),
    surfaceContainer = Color(0xFFEEEBF6),
    surfaceContainerHigh = Color(0xFFE8E5F0),
    surfaceContainerHighest = Color(0xFFE2DFEA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC6BFFF),
    onPrimary = Color(0xFF2A109C),
    primaryContainer = Color(0xFF4331D8),
    onPrimaryContainer = Color(0xFFE4DFFF),
    inversePrimary = Color(0xFF5B4CF5),
    secondary = Color(0xFFC8C3DC),
    onSecondary = Color(0xFF302F41),
    secondaryContainer = Color(0xFF464559),
    onSecondaryContainer = Color(0xFFE4E0F9),
    tertiary = Color(0xFFFFB68B),
    onTertiary = Color(0xFF532200),
    tertiaryContainer = Color(0xFF763300),
    onTertiaryContainer = Color(0xFFFFDBC8),
    background = Color(0xFF13121A),
    onBackground = Color(0xFFE5E1EC),
    surface = Color(0xFF13121A),
    onSurface = Color(0xFFE5E1EC),
    surfaceVariant = Color(0xFF47464F),
    onSurfaceVariant = Color(0xFFC9C5D0),
    surfaceTint = Color(0xFFC6BFFF),
    inverseSurface = Color(0xFFE5E1EC),
    inverseOnSurface = Color(0xFF312F38),
    outline = Color(0xFF928F9A),
    outlineVariant = Color(0xFF47464F),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3A3841),
    surfaceDim = Color(0xFF13121A),
    surfaceContainerLowest = Color(0xFF0E0D15),
    surfaceContainerLow = Color(0xFF1C1B22),
    surfaceContainer = Color(0xFF201F27),
    surfaceContainerHigh = Color(0xFF2A2932),
    surfaceContainerHighest = Color(0xFF35343D),
)

/** 添削の評価などに使う色 (良い / まあまあ / 要修正) */
@Immutable
data class GradeColors(
    val great: Color,
    val greatContainer: Color,
    val good: Color,
    val goodContainer: Color,
    val fix: Color,
    val fixContainer: Color,
) {
    fun of(rating: Rating) = when (rating) {
        Rating.GREAT -> great to greatContainer
        Rating.GOOD -> good to goodContainer
        Rating.FIX -> fix to fixContainer
    }

    fun ofScore(score: Int) = when {
        score >= 80 -> great
        score >= 60 -> good
        else -> fix
    }
}

private val LightGrades = GradeColors(
    great = Color(0xFF15803D),
    greatContainer = Color(0xFFDCF5E3),
    good = Color(0xFFB45309),
    goodContainer = Color(0xFFFFEFD5),
    fix = Color(0xFFC62828),
    fixContainer = Color(0xFFFDE4E4),
)

private val DarkGrades = GradeColors(
    great = Color(0xFF4ADE80),
    greatContainer = Color(0xFF173524),
    good = Color(0xFFFBBF24),
    goodContainer = Color(0xFF3A2E12),
    fix = Color(0xFFF87171),
    fixContainer = Color(0xFF3D1C1F),
)

val LocalGrades = staticCompositionLocalOf { LightGrades }

@Composable
fun HanasoTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors) {
        CompositionLocalProvider(LocalGrades provides if (dark) DarkGrades else LightGrades) {
            content()
        }
    }
}
