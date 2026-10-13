plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
}

// For shared modules that render UI with Compose Multiplatform (the KMP counterpart of
// android-compose-convention). Unlike kmp-library-convention this uses the Android target instead
// of `jvm()`: Compose Multiplatform artifacts resolve their desktop (skiko) variants for a plain
// JVM target, which :app and :wear must never receive. Compose artifacts come from the JetBrains
// plugin's `compose` accessor, versioned by `compose-multiplatform` in the catalog. Each module
// still sets its own `namespace` inside `kotlin { androidLibrary { … } }`.
kotlin {
    jvmToolchain(21)

    androidLibrary {
        compileSdk = rootProject.extra["COMPILE_SDK_VERSION"].toString().toInt()
        minSdk = rootProject.extra["MIN_SDK_VERSION"].toString().toInt()
        androidResources { enable = true }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.runtime)
                implementation(compose.ui)
            }
        }
    }
}
