plugins {
    alias(libs.plugins.kotlin.jvm.convention)
    alias(libs.plugins.kotlin.serialization)
}

// Pure Kotlin domain module. No `com.android.*` plugin is applied anywhere in this file, on
// purpose: that is what stops an Android or Compose import from ever compiling in here again,
// rather than relying on convention alone.
dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
}
