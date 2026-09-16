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

// Robolectric 4.17 reflects into JDK internals (jdk.internal.access.SharedSecrets and friends)
// to bootstrap ApplicationSharedMemory; on JDK 17+ that throws IllegalAccessException unless
// these packages are explicitly opened to the test JVM. See
// https://github.com/robolectric/robolectric/releases/tag/robolectric-4.17.
tasks.withType<Test>().configureEach {
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    )
}
