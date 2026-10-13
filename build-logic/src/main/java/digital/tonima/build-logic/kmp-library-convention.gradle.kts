plugins {
    id("jacoco")
    id("org.jetbrains.kotlin.multiplatform")
}

// Base setup for shared Kotlin Multiplatform modules (the iOS port). Like kotlin-jvm-convention,
// no `com.android.*` plugin is applied: a module using this convention cannot import the Android
// framework from commonMain. The `jvm()` target is what :app and :wear consume; Android-specific
// code, when a module needs it, goes behind expect/actual in a dedicated module, not here.
// iosX64 (Intel simulator) is left out on purpose: Apple-silicon simulators and devices only.
kotlin {
    jvmToolchain(21)
    jvm()
    iosArm64()
    iosSimulatorArm64()
}
