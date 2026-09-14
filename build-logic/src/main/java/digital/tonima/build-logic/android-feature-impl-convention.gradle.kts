import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.the

plugins {
    id("android-library-convention")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

val libs = the<VersionCatalogsExtension>().named("libs")

// Mirrors the shape of :core/billing/impl/build.gradle.kts exactly: Hilt + KSP + hiltbinder.
// Every :feature:*:impl (and :core:data) applies this and then does
// `implementation(project(":feature:x:bridge"))` for its own bridge, plus whatever other
// bridges/core modules it needs. Concrete classes bind to their bridge interface with
// `@Singleton @BindType(installIn = SINGLETON, to = XxxInterface::class)` instead of a
// hand-written Hilt @Module — the ksp(libs.hilt.binder.compiler) below is what generates that
// module at compile time.
dependencies {
    add("implementation", libs.findLibrary("hilt-android").get())
    add("implementation", libs.findLibrary("hilt-binder").get())
    add("ksp", libs.findLibrary("hilt-binder-compiler").get())
    add("ksp", libs.findLibrary("hilt-compiler").get())
}
