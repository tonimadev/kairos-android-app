import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.plugin.use.PluginDependency

plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

// Precompiled script plugins (the *-convention.gradle.kts files under src/main/kotlin) apply
// plugins by id only (e.g. `id("com.android.library")`), so those plugins' implementation jars
// must be on build-logic's own compile/runtime classpath. This resolves each plugin's Gradle
// "marker artifact" (<pluginId>:<pluginId>.gradle.plugin:<version>) from the same version
// catalog the rest of the app uses, so plugin versions stay defined in exactly one place
// (gradle/libs.versions.toml), matching the version this repo already builds with.
fun DependencyHandlerScope.pluginMarker(plugin: Provider<PluginDependency>) {
    val dependency = plugin.get()
    add("implementation", "${dependency.pluginId}:${dependency.pluginId}.gradle.plugin:${dependency.version}")
}

dependencies {
    pluginMarker(libs.plugins.android.library.asProvider())
    pluginMarker(libs.plugins.kotlin.compose)
    pluginMarker(libs.plugins.kotlin.ksp)
    pluginMarker(libs.plugins.hilt.android)
    pluginMarker(libs.plugins.jetbrains.kotlin.jvm)
}
