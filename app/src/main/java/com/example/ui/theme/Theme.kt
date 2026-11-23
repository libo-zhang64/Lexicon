package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val BrightDaylightColorScheme = lightColorScheme(
    primary = VividCobaltPrimary,
    onPrimary = OnCobaltPrimary,
    primaryContainer = SkyBlueContainer,
    onPrimaryContainer = OnSkyBlueContainer,
    secondary = SunlitAmberSecondary,
    onSecondary = OnAmberSecondary,
    secondaryContainer = WarmSunContainer,
    onSecondaryContainer = OnWarmSunContainer,
    tertiary = FreshEmeraldTertiary,
    onTertiary = OnEmeraldTertiary,
    tertiaryContainer = MintWashContainer,
    onTertiaryContainer = OnMintWashContainer,
    background = DaylightBackground,
    onBackground = CrispInkText,
    surface = DaylightBackground,
    onSurface = CrispInkText,
    surfaceVariant = SoftSkySurfaceLow,
    onSurfaceVariant = SlateSecondaryText,
    surfaceContainerLowest = BrightCardSurface,
    surfaceContainerLow = SoftSkySurfaceLow,
    surfaceContainer = CrispTintSurface,
    surfaceContainerHigh = CrispTintSurfaceHigh,
    outline = SoftBorderOutline,
    outlineVariant = SubtleHairline,
    error = CrispCoralError,
    errorContainer = CrispCoralErrorContainer,
    onErrorContainer = OnCrispCoralErrorContainer
)

private val CrispTwilightColorScheme = darkColorScheme(
    primary = BrightSkyPrimary,
    onPrimary = OnBrightSkyPrimary,
    primaryContainer = DeepSkyContainer,
    onPrimaryContainer = OnDeepSkyContainer,
    secondary = SoftGoldSecondary,
    onSecondary = OnSoftGoldSecondary,
    secondaryContainer = DeepGoldContainer,
    onSecondaryContainer = OnDeepGoldContainer,
    tertiary = BrightMintTertiary,
    onTertiary = OnBrightMintTertiary,
    tertiaryContainer = DeepMintContainer,
    onTertiaryContainer = OnDeepMintContainer,
    background = TwilightBackground,
    onBackground = BrightIvoryInk,
    surface = TwilightBackground,
    onSurface = BrightIvoryInk,
    surfaceVariant = TwilightSurfaceLow,
    onSurfaceVariant = CoolMutedText,
    surfaceContainerLowest = TwilightCard,
    surfaceContainerLow = TwilightSurfaceLow,
    surfaceContainer = TwilightCard,
    surfaceContainerHigh = TwilightSurfaceHigh,
    outline = TwilightBorder,
    outlineVariant = TwilightBorder
)

val LexiconShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LexiconTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CrispTwilightColorScheme else BrightDaylightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LexiconTypography,
        shapes = LexiconShapes,
        content = content
    )
}
