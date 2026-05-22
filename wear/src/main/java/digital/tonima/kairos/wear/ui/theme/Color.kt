package digital.tonima.kairos.wear.ui.theme

import androidx.compose.ui.graphics.Color

// Updated colors for the new alarm design
val PrimaryDark = Color(0xFFDEFA5F) // Neon Yellow
val OnPrimaryDark = Color(0xFF000000)
val PrimaryContainerDark = Color(0xFF42425A)
val OnPrimaryContainerDark = Color(0xFFFFFFFF)

val SecondaryDark = Color(0xFFDEFA5F)
val OnSecondaryDark = Color(0xFF000000)
val SecondaryContainerDark = Color(0xFF42425A)
val OnSecondaryContainerDark = Color(0xFFFFFFFF)

val TertiaryDark = Color(0xFFFB7185)
val OnTertiaryDark = Color(0xFF4C0519)
val TertiaryContainerDark = Color(0xFF9F1239)
val OnTertiaryContainerDark = Color(0xFFFFE4E6)

val SurfaceDark = Color(0xFF25252D) // Dark Background
val OnSurfaceDark = Color(0xFFFFFFFF)
val SurfaceVariantDark = Color(0xFF323242) // Card Background
val OnSurfaceVariantDark = Color(0xFFB0B0C0) // Subtext / Muted
val OutlineDark = Color(0xFF42425A)
val OutlineLight = Color(0xFFCBD5E1)

object ColorTokens {
    val Primary = PrimaryDark
    val PrimaryDim = Color(0xFFC0D940)
    val PrimaryContainer = PrimaryContainerDark
    val OnPrimary = OnPrimaryDark
    val OnPrimaryContainer = OnPrimaryContainerDark

    val Secondary = SecondaryDark
    val SecondaryDim = Color(0xFFC0D940)
    val SecondaryContainer = SecondaryContainerDark
    val OnSecondary = OnSecondaryDark
    val OnSecondaryContainer = OnSecondaryContainerDark

    val Tertiary = TertiaryDark
    val TertiaryDim = Color(0xFFE25A6E)
    val TertiaryContainer = TertiaryContainerDark
    val OnTertiary = OnTertiaryDark
    val OnTertiaryContainer = OnTertiaryContainerDark

    val Background = SurfaceDark
    val OnBackground = OnSurfaceDark

    val SurfaceContainerLow = Color(0xFF1E1E26)
    val SurfaceContainer = SurfaceDark
    val SurfaceContainerHigh = SurfaceVariantDark

    val OnSurface = OnSurfaceDark
    val OnSurfaceVariant = OnSurfaceVariantDark

    val Outline = OutlineDark
    val OutlineVariant = Color(0xFF2C2C3A)

    val Error = Color(0xFFF2B8B5)
    val ErrorDim = Color(0xFFE0B4B1)
    val ErrorContainer = Color(0xFF8C1D18)
    val OnError = Color.Black
    val OnErrorContainer = Color(0xFFF2B8B5)
}
