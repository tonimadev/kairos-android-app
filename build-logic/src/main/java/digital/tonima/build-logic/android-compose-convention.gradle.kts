import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.the

plugins {
    id("android-library-convention")
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = the<VersionCatalogsExtension>().named("libs")

// For modules that render UI (:core:ui and every :feature:*:impl). Deliberately not part of
// android-library-convention: :core:navigation and every :feature:*:bridge apply only the base
// convention (plus androidx.compose.runtime as a plain library dep where a NavKey or an
// @Immutable UiState needs it), never the Compose compiler plugin, since they must contain
// no UI.
android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    val bom = platform(libs.findLibrary("androidx-compose-bom").get())
    add("implementation", bom)
    add("androidTestImplementation", bom)
}
