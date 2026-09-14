plugins {
    id("com.android.library")
}

// Base Android-library setup shared by every new module (:core:*, :feature:*:bridge,
// :feature:*:impl). Mirrors what :core/build.gradle.kts and :core/billing/*/build.gradle.kts
// already set up by hand today. Each consuming module still sets its own `namespace`.
android {
    compileSdk = rootProject.extra["COMPILE_SDK_VERSION"].toString().toInt()

    defaultConfig {
        minSdk = rootProject.extra["MIN_SDK_VERSION"].toString().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin { jvmToolchain(21) }
}
