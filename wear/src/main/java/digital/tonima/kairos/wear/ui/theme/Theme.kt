package digital.tonima.kairos.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme

private val DarkColorScheme =
    ColorScheme(
        primary = ColorTokens.Primary,
        primaryDim = ColorTokens.PrimaryDim,
        primaryContainer = ColorTokens.PrimaryContainer,
        onPrimary = ColorTokens.OnPrimary,
        onPrimaryContainer = ColorTokens.OnPrimaryContainer,
        secondary = ColorTokens.Secondary,
        secondaryDim = ColorTokens.SecondaryDim,
        secondaryContainer = ColorTokens.SecondaryContainer,
        onSecondary = ColorTokens.OnSecondary,
        onSecondaryContainer = ColorTokens.OnSecondaryContainer,
        tertiary = ColorTokens.Tertiary,
        tertiaryDim = ColorTokens.TertiaryDim,
        tertiaryContainer = ColorTokens.TertiaryContainer,
        onTertiary = ColorTokens.OnTertiary,
        onTertiaryContainer = ColorTokens.OnTertiaryContainer,
        surfaceContainerLow = ColorTokens.SurfaceContainerLow,
        surfaceContainer = ColorTokens.SurfaceContainer,
        surfaceContainerHigh = ColorTokens.SurfaceContainerHigh,
        onSurface = ColorTokens.OnSurface,
        onSurfaceVariant = ColorTokens.OnSurfaceVariant,
        outline = ColorTokens.Outline,
        outlineVariant = ColorTokens.OutlineVariant,
        background = ColorTokens.Background,
        onBackground = ColorTokens.OnBackground,
        error = ColorTokens.Error,
        errorDim = ColorTokens.ErrorDim,
        errorContainer = ColorTokens.ErrorContainer,
        onError = ColorTokens.OnError,
        onErrorContainer = ColorTokens.OnErrorContainer,
    )

@Composable
fun KairosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
