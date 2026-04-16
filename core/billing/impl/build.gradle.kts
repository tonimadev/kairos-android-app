plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.ksp)
}

android {
    namespace = "digital.tonima.kairos.core.billing.impl"
    compileSdk = rootProject.extra["COMPILE_SDK_VERSION"].toString().toInt()

    defaultConfig {
        minSdk = rootProject.extra["MIN_SDK_VERSION"].toString().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin { jvmToolchain(21) }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.billing.ktx)
    implementation(libs.hilt.android)
    implementation(libs.hilt.binder)
    implementation(libs.logcat)
    implementation(project(":core:billing:bridge"))

    ksp(libs.hilt.binder.compiler)
    ksp(libs.hilt.compiler)
}
