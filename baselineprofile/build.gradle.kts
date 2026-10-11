plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "digital.tonima.kairos.baselineprofile"
    compileSdk = rootProject.extra["COMPILE_SDK_VERSION"].toString().toInt()

    defaultConfig {
        minSdk = rootProject.extra["MIN_SDK_VERSION"].toString().toInt()
        targetSdk = rootProject.extra["TARGET_SDK_VERSION"].toString().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // In-process (Perfetto SDK) tracing and method tracing start the app with a broadcast right
        // before each cold start; that races with the measured launch and is not needed for TTID.
        testInstrumentationRunnerArguments["androidx.benchmark.perfettoSdkTracing.enable"] = "false"
        testInstrumentationRunnerArguments["androidx.benchmark.fullTracing.enable"] = "false"
        testInstrumentationRunnerArguments["androidx.benchmark.profiling.mode"] = "None"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    targetProjectPath = ":app"
}

kotlin {
    jvmToolchain(21)
}

// Profiles and benchmarks run on the connected device or emulator.
baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.uiautomator)
}
