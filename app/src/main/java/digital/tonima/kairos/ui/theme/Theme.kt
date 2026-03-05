package digital.tonima.kairos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = Blue80,
        onPrimary = Color(0xFF003366),
        primaryContainer = Color(0xFF1A3A5C),
        onPrimaryContainer = Blue80,
        secondary = Cyan80,
        onSecondary = Color(0xFF00363C),
        secondaryContainer = Color(0xFF1A3A3E),
        onSecondaryContainer = Cyan80,
        tertiary = BlueGrey80,
        background = SurfaceDark,
        surface = SurfaceDark,
        surfaceVariant = CardDark,
        onBackground = Color(0xFFE3EEFF),
        onSurface = Color(0xFFE3EEFF),
        onSurfaceVariant = Color(0xFFB0C4DE),
        outline = Color(0xFF6A8AAA),
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Blue40,
        onPrimary = Color.White,
        primaryContainer = CardLight,
        onPrimaryContainer = Blue40,
        secondary = Cyan40,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFB2EBF2),
        onSecondaryContainer = Color(0xFF00363C),
        tertiary = BlueGrey40,
        background = SurfaceLight,
        surface = Color.White,
        surfaceVariant = CardLight,
        onBackground = Color(0xFF0D1B2A),
        onSurface = Color(0xFF0D1B2A),
        onSurfaceVariant = BlueGrey40,
        outline = Color(0xFF7A9BBB),
    )

@Composable
fun KairosTheme(
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
