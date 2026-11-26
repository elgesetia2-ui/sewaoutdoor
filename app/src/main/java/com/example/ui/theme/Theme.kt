package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SewaOutdoorLightColorScheme = lightColorScheme(
    primary = MaroonPrimary,
    onPrimary = PureWhite,
    primaryContainer = MaroonContainer,
    onPrimaryContainer = OnMaroonContainer,
    secondary = MaroonLight,
    onSecondary = PureWhite,
    secondaryContainer = SurfaceMuted,
    onSecondaryContainer = TextPrimary,
    tertiary = SandAccent,
    onTertiary = OnSandContainer,
    tertiaryContainer = SandContainer,
    onTertiaryContainer = OnSandContainer,
    background = AppBackground,
    onBackground = TextPrimary,
    surface = SurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = TextSecondary,
    outline = OutlineSubtle,
    outlineVariant = OutlineSubtle,
    error = DangerRed,
    onError = PureWhite,
    errorContainer = DangerContainer,
    onErrorContainer = DangerRed
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SewaOutdoorTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SewaOutdoorLightColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
