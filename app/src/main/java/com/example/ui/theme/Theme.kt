package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val EditorialLightColorScheme = lightColorScheme(
    primary = SlateInkTeal,
    onPrimary = OnSlateInkTeal,
    primaryContainer = IceWashContainer,
    onPrimaryContainer = OnIceWash,
    secondary = EditorialTerracotta,
    onSecondary = OnTerracotta,
    secondaryContainer = TerracottaWash,
    onSecondaryContainer = OnTerracottaWash,
    tertiary = SageLeaf,
    onTertiary = OnSageLeaf,
    tertiaryContainer = SageWash,
    onTertiaryContainer = OnSageWash,
    background = PaperBackground,
    onBackground = DeepPrintingInk,
    surface = PaperBackground,
    onSurface = DeepPrintingInk,
    surfaceVariant = LinenSurfaceLow,
    onSurfaceVariant = GraphitePencil,
    surfaceContainerLowest = WarmWhiteCard,
    surfaceContainerLow = LinenSurfaceLow,
    surfaceContainer = ParchmentSurface,
    surfaceContainerHigh = ParchmentSurfaceHigh,
    outline = MutedInkOutline,
    outlineVariant = HairlineRule,
    error = EditorialError,
    errorContainer = EditorialErrorContainer
)

private val EditorialDarkColorScheme = darkColorScheme(
    primary = SoftInkTeal,
    onPrimary = OnSoftInkTeal,
    primaryContainer = DarkTealWash,
    onPrimaryContainer = OnDarkTealWash,
    secondary = MutedClay,
    onSecondary = OnMutedClay,
    secondaryContainer = DarkClayWash,
    onSecondaryContainer = OnDarkClayWash,
    tertiary = SoftSage,
    onTertiary = OnSoftSage,
    tertiaryContainer = DarkSageWash,
    onTertiaryContainer = OnDarkSageWash,
    background = CharcoalFolio,
    onBackground = WarmIvoryInk,
    surface = CharcoalFolio,
    onSurface = WarmIvoryInk,
    surfaceVariant = DarkSlateLow,
    onSurfaceVariant = SoftAshText,
    surfaceContainerLowest = CharcoalFolio,
    surfaceContainerLow = DarkSlateLow,
    surfaceContainer = DarkSlateCard,
    surfaceContainerHigh = DarkSlateHigh,
    outline = DarkRule,
    outlineVariant = DarkRule
)

val LexiconShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun LexiconTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) EditorialDarkColorScheme else EditorialLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = LexiconTypography,
        shapes = LexiconShapes,
        content = content
    )
}
