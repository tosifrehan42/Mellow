package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemePreset

val LocalMelloPalette = staticCompositionLocalOf { SoftLavenderPalette }
val LocalReducedMotion = staticCompositionLocalOf { false }

val MelloShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

fun paletteForPreset(preset: ThemePreset): MelloPalette = when (preset) {
    ThemePreset.SOFT_LAVENDER -> SoftLavenderPalette
    ThemePreset.ROSE_QUARTZ -> RoseQuartzPalette
    ThemePreset.SAGE_GREEN -> SageGreenPalette
    ThemePreset.BUTTER_CREAM -> ButterCreamPalette
    ThemePreset.MIDNIGHT -> MidnightPalette
    ThemePreset.MINIMAL_MONOCHROME -> MinimalMonochromePalette
}

fun MelloPalette.cardPastelPair(colorKey: String): Pair<Color, Color> = when (colorKey.lowercase()) {
    "lavender", "work" -> pastelLavender to pastelLavenderText
    "pink", "personal" -> pastelPink to pastelPinkText
    "mint", "wellness" -> pastelMint to pastelMintText
    "butter", "errands" -> pastelButter to pastelButterText
    "sky", "study" -> pastelSky to pastelSkyText
    "peach" -> pastelPeach to pastelPeachText
    else -> pastelLavender to pastelLavenderText
}

fun MelloPalette.priorityColors(priority: String): Pair<Color, Color> = when (priority.lowercase()) {
    "high" -> priorityHighBg to priorityHighText
    "medium" -> priorityMedBg to priorityMedText
    else -> priorityLowBg to priorityLowText
}

object MelloThemeColors {
    val palette: MelloPalette
        @Composable
        @ReadOnlyComposable
        get() = LocalMelloPalette.current

    val reducedMotion: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalReducedMotion.current
}

@Composable
fun MelloTheme(
    preset: ThemePreset = ThemePreset.SOFT_LAVENDER,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val palette = paletteForPreset(preset)
    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primarySoft,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.pastelMintText,
            onSecondary = palette.surface,
            secondaryContainer = palette.pastelMint,
            onSecondaryContainer = palette.pastelMintText,
            tertiary = palette.pastelPinkText,
            tertiaryContainer = palette.pastelPink,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceMuted,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.danger
        )
    } else {
        lightColorScheme(
            primary = palette.primary,
            onPrimary = palette.onPrimary,
            primaryContainer = palette.primarySoft,
            onPrimaryContainer = palette.textPrimary,
            secondary = palette.pastelMintText,
            onSecondary = palette.surface,
            secondaryContainer = palette.pastelMint,
            onSecondaryContainer = palette.pastelMintText,
            tertiary = palette.pastelPinkText,
            tertiaryContainer = palette.pastelPink,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.surface,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.surfaceMuted,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.danger
        )
    }

    CompositionLocalProvider(
        LocalMelloPalette provides palette,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MelloTypography,
            shapes = MelloShapes,
            content = content
        )
    }
}

// Keep compatibility alias for existing preview/test references
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MelloTheme(content = content)
}
