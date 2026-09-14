import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.the

plugins {
    id("android-library-convention")
}

val libs = the<VersionCatalogsExtension>().named("libs")

// Mirrors the shape of :core/billing/bridge/build.gradle.kts exactly: an Android library with
// nothing else on top. No Hilt, no KSP, no Compose compiler. A :feature:*:bridge module holds
// only contracts (interfaces, NavKeys, plain data classes) — if a module applying this plugin
// ever needs Hilt or Compose, that is a sign UI or a ViewModel leaked into the bridge and
// belongs in the matching :impl module instead.
dependencies {
    add("implementation", libs.findLibrary("androidx-core-ktx").get())
}
